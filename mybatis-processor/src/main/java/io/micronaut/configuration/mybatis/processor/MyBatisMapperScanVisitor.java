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
import java.util.TreeMap;
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
    private final Set<String> generatedRegistrations = new HashSet<>();

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
        // Mappers are grouped by package and every group is written to a registration class in that package,
        // because a class literal of a non-public interface can only be used from the same package. Explicitly
        // listed mappers are referenced from the annotation, so they are accessible from the annotated type.
        Map<String, Set<String>> mapperTypesByPackage = new TreeMap<>();
        mapperTypesByPackage.computeIfAbsent(element.getPackageName(), ignored -> new TreeSet<>()).addAll(scan.mappers());
        List<String> unresolvedPackages = new ArrayList<>();
        for (String packageName : scan.packages()) {
            Set<String> discovered = discoverMappers(packageName);
            if (discovered.isEmpty()) {
                unresolvedPackages.add(packageName);
                context.warn("No mapper interface found in package [" + packageName + "] during compilation. "
                    + "Mapper interfaces from other modules must be listed in the `mappers` member of @"
                    + MyBatisMapperScan.class.getSimpleName()
                    + "; MyBatis runtime scanning is used as a fallback, which is not supported in GraalVM native images.", element);
            }
            for (String mapperType : discovered) {
                mapperTypesByPackage.computeIfAbsent(packageOf(mapperType), ignored -> new TreeSet<>()).add(mapperType);
            }
        }
        if (mapperTypesByPackage.values().stream().allMatch(Set::isEmpty) && unresolvedPackages.isEmpty()) {
            context.warn("@" + MyBatisMapperScan.class.getSimpleName() + " declares neither packages nor mappers", element);
            return;
        }
        for (Map.Entry<String, Set<String>> entry : mapperTypesByPackage.entrySet()) {
            String packageName = entry.getKey();
            Set<String> mapperTypes = entry.getValue();
            List<String> packages = packageName.equals(element.getPackageName()) ? unresolvedPackages : List.of();
            if (mapperTypes.isEmpty() && packages.isEmpty()) {
                continue;
            }
            ClassDef registration = registrationDefinition(element, packageName, scan.datasource(), mapperTypes, packages);
            if (!generatedRegistrations.add(registration.getName())) {
                context.fail("The generated registration [" + registration.getName() + "] of @"
                    + MyBatisMapperScan.class.getSimpleName() + " on [" + element.getName()
                    + "] clashes with the registration of another annotated type. Rename one of the annotated types.", element);
                return;
            }
            writeClass(context, element, registration);
            context.visitServiceDescriptor(MyBatisMapperScanRegistration.class, registration.getName(), element);
        }
    }

    /**
     * The name of the registration class of an annotated type written to the given package. Registrations written
     * to the package of the annotated type are named after its simple name, those written to a mapper package after
     * its fully qualified name, both {@link #encode(String) encoded} so that distinct types always yield distinct
     * registration names.
     */
    private static String registrationName(ClassElement element, String packageName) {
        String elementPackage = element.getPackageName();
        String qualifier = packageName.equals(elementPackage)
            ? element.getName().substring(elementPackage.isEmpty() ? 0 : elementPackage.length() + 1)
            : element.getName();
        String simpleName = encode(qualifier) + REGISTRATION_SUFFIX;
        return packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
    }

    /**
     * Turns a type name into an identifier fragment with a prefix-free, and therefore injective, encoding:
     * {@code _} becomes {@code _u}, {@code .} becomes {@code _p} and {@code $} becomes {@code _d}. Every
     * underscore of the result starts an escape sequence, so e.g. {@code a.b_.c} and {@code a.b._c}, or
     * {@code Outer$Inner} and {@code Outer_Inner}, are encoded differently.
     */
    private static String encode(String typeName) {
        return typeName.replace("_", "_u").replace(".", "_p").replace("$", "_d");
    }

    private static String packageOf(String typeName) {
        int lastDot = typeName.lastIndexOf('.');
        return lastDot > 0 ? typeName.substring(0, lastDot) : "";
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
                                                   String packageName,
                                                   String datasource,
                                                   Set<String> mapperTypes,
                                                   List<String> unresolvedPackages) {
        String registrationName = registrationName(element, packageName);
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
