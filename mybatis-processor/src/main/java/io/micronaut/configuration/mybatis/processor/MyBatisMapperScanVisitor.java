/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.mybatis.processor;

import io.micronaut.configuration.mybatis.MyBatisMapperScan;
import io.micronaut.configuration.mybatis.MyBatisMapperScanRegistration;
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.inject.writer.GeneratedFile;
import io.micronaut.sourcegen.bytecode.ByteCodeWriter;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import org.apache.ibatis.session.Configuration;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Generates a {@link MyBatisMapperScanRegistration} for every type annotated with {@link MyBatisMapperScan}.
 *
 * <p>The visitor deliberately visits every class (the default {@code "*"} of
 * {@link #getSupportedAnnotationNames()}) because it has to see the mapper interfaces, which carry no
 * annotation, in addition to the annotated types. Only interfaces compiled in the same compilation
 * unit can be discovered by package; for packages without any discovered interface a warning is
 * emitted and the generated registration falls back to MyBatis runtime scanning.</p>
 */
@Internal
public final class MyBatisMapperScanVisitor implements TypeElementVisitor<Object, Object> {

    private static final String REGISTRATION_SUFFIX = "$MyBatisMapperScanRegistration";

    /**
     * The bytecode writer is used directly instead of {@code ByteCodeGenerator} from
     * {@code micronaut-sourcegen-generator-bytecode}: that module registers a {@code SourceGenerator} service for
     * Java, which other processors (e.g. Serde) on the same classpath may pick up with a mismatching version. The
     * writer is already a dependency of {@code micronaut-core-processor}, so no additional sourcegen artifact is
     * put on the annotation processor classpath.
     */
    private static final ByteCodeWriter BYTE_CODE_WRITER = new ByteCodeWriter(false, true);

    private final Set<String> interfaceTypes = new LinkedHashSet<>();
    private final Map<String, Scan> scans = new LinkedHashMap<>();
    private final Set<String> written = new HashSet<>();

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.AGGREGATING;
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        collectInterfaces(element);
        AnnotationValue<MyBatisMapperScan> annotation = element.getAnnotation(MyBatisMapperScan.class);
        if (annotation != null) {
            List<String> mappers = new ArrayList<>();
            for (AnnotationClassValue<?> mapper : annotation.annotationClassValues("mappers")) {
                mappers.add(mapper.getName());
            }
            scans.put(element.getName(), new Scan(
                element.getName(),
                List.of(annotation.stringValues("value")),
                mappers,
                annotation.stringValue("datasource").orElse("default"),
                annotation.booleanValue("nativeImageMetadata").orElse(true)
            ));
        }
    }

    @Override
    public void finish(VisitorContext context) {
        Set<String> proxyTypes = new TreeSet<>();
        Set<String> reflectiveTypes = new TreeSet<>();
        ClassElement originatingElement = null;
        for (Scan scan : scans.values()) {
            if (!written.add(scan.elementName())) {
                continue;
            }
            ClassElement element = context.getClassElement(scan.elementName()).orElse(null);
            if (element == null) {
                continue;
            }
            Set<String> mapperTypes = new TreeSet<>(scan.mappers());
            List<String> unresolvedPackages = new ArrayList<>();
            for (String packageName : scan.packages()) {
                Set<String> discovered = discoverMappers(packageName);
                if (discovered.isEmpty()) {
                    unresolvedPackages.add(packageName);
                    context.warn("No mapper interface found in package [" + packageName + "] during compilation. "
                        + "Mapper interfaces from other modules must be listed in the `mappers` member of @"
                        + MyBatisMapperScan.class.getSimpleName()
                        + "; MyBatis runtime scanning is used as a fallback, which is not supported in GraalVM native images.", element);
                } else {
                    mapperTypes.addAll(discovered);
                }
            }
            if (mapperTypes.isEmpty() && unresolvedPackages.isEmpty()) {
                context.warn("@" + MyBatisMapperScan.class.getSimpleName() + " declares neither packages nor mappers", element);
                continue;
            }
            ClassDef registration = registrationDefinition(element, scan.datasource(), mapperTypes, unresolvedPackages);
            writeClass(context, element, registration);
            context.visitServiceDescriptor(MyBatisMapperScanRegistration.class, registration.getName(), element);

            if (scan.nativeImageMetadata()) {
                originatingElement = element;
                proxyTypes.addAll(mapperTypes);
                for (String mapperType : mapperTypes) {
                    context.getClassElement(mapperType).ifPresent(mapper -> collectReflectiveTypes(mapper, reflectiveTypes));
                }
            }
        }
        if (originatingElement != null) {
            writeNativeImageMetadata(context, originatingElement, proxyTypes, reflectiveTypes);
        }
    }

    private static void writeClass(VisitorContext context, ClassElement originatingElement, ClassDef classDef) {
        try (OutputStream outputStream = context.visitClass(classDef.getName(), originatingElement)) {
            outputStream.write(BYTE_CODE_WRITER.write(classDef, null));
        } catch (IOException e) {
            throw new ProcessingException(originatingElement, "Failed to generate '" + classDef.getName() + "': " + e.getMessage(), e);
        }
    }

    /**
     * MyBatis implements mapper interfaces with {@link java.lang.reflect.Proxy} and instantiates and populates
     * result and parameter objects reflectively. Both need GraalVM metadata, which is written next to the
     * generated registration so that users do not have to declare it by hand.
     */
    private static void writeNativeImageMetadata(VisitorContext context,
                                                 ClassElement originatingElement,
                                                 Set<String> proxyTypes,
                                                 Set<String> reflectiveTypes) {
        Map<String, String> options = context.getOptions();
        String group = options.getOrDefault(VisitorContext.MICRONAUT_PROCESSING_GROUP, originatingElement.getPackageName());
        String module = options.getOrDefault(VisitorContext.MICRONAUT_PROCESSING_MODULE, "mybatis-mapper-scan");
        String directory = "native-image/" + group + "/" + module + "/";

        StringBuilder proxyConfig = new StringBuilder("[\n");
        for (String proxyType : proxyTypes) {
            proxyConfig.append("  {\"interfaces\": [\"").append(proxyType).append("\"]},\n");
        }
        writeMetaInfFile(context, originatingElement, directory + "proxy-config.json", closeJsonArray(proxyConfig));

        if (!reflectiveTypes.isEmpty()) {
            StringBuilder reflectConfig = new StringBuilder("[\n");
            for (String reflectiveType : reflectiveTypes) {
                reflectConfig.append("  {\"name\": \"").append(reflectiveType).append("\", ")
                    .append("\"allDeclaredConstructors\": true, \"allPublicConstructors\": true, ")
                    .append("\"allDeclaredMethods\": true, \"allPublicMethods\": true, ")
                    .append("\"allDeclaredFields\": true, \"allPublicFields\": true},\n");
            }
            writeMetaInfFile(context, originatingElement, directory + "reflect-config.json", closeJsonArray(reflectConfig));
        }
    }

    private static String closeJsonArray(StringBuilder json) {
        int trailingComma = json.lastIndexOf(",");
        if (trailingComma > 0) {
            json.deleteCharAt(trailingComma);
        }
        return json.append("]\n").toString();
    }

    private static void writeMetaInfFile(VisitorContext context, ClassElement originatingElement, String path, String content) {
        try {
            GeneratedFile file = context.visitMetaInfFile(path, originatingElement).orElse(null);
            if (file == null) {
                return;
            }
            try (Writer writer = file.openWriter()) {
                writer.write(content);
            }
        } catch (IOException e) {
            context.warn("Unable to write GraalVM metadata file [META-INF/" + path + "]: " + e.getMessage(), originatingElement);
        }
    }

    /**
     * Collects the result and parameter types of the mapper methods, unwrapping containers.
     */
    private static void collectReflectiveTypes(ClassElement mapper, Set<String> reflectiveTypes) {
        for (MethodElement method : mapper.getEnclosedElements(ElementQuery.ALL_METHODS)) {
            addReflectiveType(method.getGenericReturnType(), reflectiveTypes);
            for (ParameterElement parameter : method.getParameters()) {
                addReflectiveType(parameter.getGenericType(), reflectiveTypes);
            }
        }
    }

    private static void addReflectiveType(ClassElement type, Set<String> reflectiveTypes) {
        if (type == null || type.isPrimitive() || type.isEnum()) {
            return;
        }
        if (type.isArray()) {
            addReflectiveType(type.fromArray(), reflectiveTypes);
            return;
        }
        if (type.isAssignable(Iterable.class)
            || type.isAssignable(Map.class)
            || type.isAssignable(Optional.class)
            || type.isAssignable("java.util.stream.Stream")
            || type.isAssignable("org.reactivestreams.Publisher")) {
            for (ClassElement typeArgument : type.getTypeArguments().values()) {
                addReflectiveType(typeArgument, reflectiveTypes);
            }
            return;
        }
        String name = type.getName();
        if (name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jakarta.")
            || name.startsWith("kotlin.") || name.startsWith("groovy.")
            || name.startsWith("org.apache.ibatis.")) {
            return;
        }
        reflectiveTypes.add(name);
    }

    /**
     * Collects the element and its nested types when they are interfaces. Nested interfaces are included
     * because MyBatis runtime package scanning registers them as well.
     */
    private void collectInterfaces(ClassElement element) {
        if (element.isInterface() && !element.isAssignable(Annotation.class)) {
            interfaceTypes.add(element.getName());
        }
        for (ClassElement inner : element.getEnclosedElements(ElementQuery.ALL_INNER_CLASSES)) {
            collectInterfaces(inner);
        }
    }

    private Set<String> discoverMappers(String packageName) {
        Set<String> discovered = new TreeSet<>();
        for (String interfaceType : interfaceTypes) {
            String interfacePackage = packageOf(interfaceType);
            if (interfacePackage.equals(packageName) || interfacePackage.startsWith(packageName + ".")) {
                discovered.add(interfaceType);
            }
        }
        return discovered;
    }

    private static ClassDef registrationDefinition(ClassElement element,
                                                   String datasource,
                                                   Set<String> mapperTypes,
                                                   List<String> unresolvedPackages) {
        String packageName = element.getPackageName();
        String simpleName = element.getName().substring(packageName.isEmpty() ? 0 : packageName.length() + 1)
            .replace('$', '_') + REGISTRATION_SUFFIX;
        String registrationName = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
        return ClassDef.builder(registrationName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(ClassTypeDef.of(MyBatisMapperScanRegistration.class))
            .addMethod(MethodDef.builder("getDatasourceName")
                .overrides()
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .build((aThis, parameters) -> ExpressionDef.constant(datasource).returning()))
            .addMethod(MethodDef.builder("register")
                .overrides()
                .addModifiers(Modifier.PUBLIC)
                .addParameter("configuration", Configuration.class)
                .build((aThis, parameters) -> {
                    ExpressionDef configuration = parameters.get(0);
                    List<StatementDef> statements = new ArrayList<>();
                    for (String mapperType : mapperTypes) {
                        statements.add(aThis.invoke("addMapper", TypeDef.VOID,
                            configuration, ExpressionDef.constant(ClassTypeDef.of(mapperType))));
                    }
                    for (String unresolvedPackage : unresolvedPackages) {
                        statements.add(aThis.invoke("addMappers", TypeDef.VOID,
                            configuration, ExpressionDef.constant(unresolvedPackage)));
                    }
                    return StatementDef.multi(statements);
                }))
            .build();
    }

    private static String packageOf(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot > 0 ? typeName.substring(0, lastDot) : "";
    }

    private record Scan(String elementName,
                        List<String> packages,
                        List<String> mappers,
                        String datasource,
                        boolean nativeImageMetadata) {
    }
}
