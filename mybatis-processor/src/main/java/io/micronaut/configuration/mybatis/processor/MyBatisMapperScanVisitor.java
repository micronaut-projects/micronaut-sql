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
import io.micronaut.configuration.mybatis.processor.MapperScanSupport.Scan;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.processing.ProcessingException;
import io.micronaut.inject.visitor.VisitorContext;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
 *
 * <p>The GraalVM native image metadata of the mappers is handled by {@link MyBatisMapperScanReflectionVisitor}.</p>
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
        List<ClassElement> interfaces = new ArrayList<>();
        MapperScanSupport.collectInterfaces(element, interfaces);
        for (ClassElement anInterface : interfaces) {
            interfaceTypes.add(anInterface.getName());
        }
        Scan scan = MapperScanSupport.readScan(element);
        if (scan != null) {
            scans.put(element.getName(), scan);
        }
    }

    @Override
    public void finish(VisitorContext context) {
        for (Scan scan : scans.values()) {
            if (written.add(scan.element().getName())) {
                generateRegistration(context, scan);
            }
        }
    }

    private void generateRegistration(VisitorContext context, Scan scan) {
        ClassElement element = scan.element();
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
            return;
        }
        ClassDef registration = registrationDefinition(element, scan.datasource(), mapperTypes, unresolvedPackages);
        writeClass(context, element, registration);
        context.visitServiceDescriptor(MyBatisMapperScanRegistration.class, registration.getName(), element);
    }

    private static void writeClass(VisitorContext context, ClassElement originatingElement, ClassDef classDef) {
        try (OutputStream outputStream = context.visitClass(classDef.getName(), originatingElement)) {
            outputStream.write(BYTE_CODE_WRITER.write(classDef, null));
        } catch (IOException e) {
            throw new ProcessingException(originatingElement, "Failed to generate '" + classDef.getName() + "': " + e.getMessage(), e);
        }
    }

    private Set<String> discoverMappers(String packageName) {
        Set<String> discovered = new TreeSet<>();
        for (String interfaceType : interfaceTypes) {
            if (MapperScanSupport.isInPackage(interfaceType, packageName)) {
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
}
