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
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import io.micronaut.sourcegen.generator.bytecode.ByteCodeGenerator;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import jakarta.inject.Named;
import org.apache.ibatis.session.Configuration;

import javax.lang.model.element.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Generates registrations for mapper types discovered at compile time.
 */
public final class MyBatisMapperScanVisitor implements TypeElementVisitor<Object, Object> {

    private static final ByteCodeGenerator BYTE_CODE_GENERATOR = new ByteCodeGenerator();

    private final Set<String> mapperTypes = new LinkedHashSet<>();
    private final Map<String, Scan> scans = new LinkedHashMap<>();
    private boolean processed;

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.AGGREGATING;
    }

    @Override
    public Set<String> getSupportedAnnotationNames() {
        return Set.of(MyBatisMapperScan.class.getName());
    }

    @Override
    public void visitClass(ClassElement element, VisitorContext context) {
        if (element.isInterface()) {
            mapperTypes.add(element.getName());
        }

        AnnotationValue<MyBatisMapperScan> scan = element.getAnnotation(MyBatisMapperScan.class);
        if (scan != null) {
            scans.put(element.getName(), new Scan(
                element.getName(),
                scan.stringValues("value"),
                scan.stringValue("datasource").orElse("default")
            ));
        }
    }

    @Override
    public void finish(VisitorContext context) {
        if (processed || scans.isEmpty()) {
            return;
        }
        processed = true;

        for (Scan scan : scans.values()) {
            ClassElement element = context.getClassElement(scan.elementName()).orElse(null);
            if (element == null) {
                continue;
            }
            Set<String> packages = new TreeSet<>(Arrays.asList(scan.packages()));

            Set<String> selectedMapperTypes = new TreeSet<>();
            for (String mapperType : mapperTypes) {
                if (isInScannedPackage(mapperType, packages)) {
                    selectedMapperTypes.add(mapperType);
                }
            }

            element.annotate(Named.class, builder -> builder.value(scan.datasource()));
            writeRegistrations(context, element, scan, selectedMapperTypes);
        }
    }

    private void writeRegistrations(VisitorContext context,
                                    ClassElement element,
                                    Scan scan,
                                    Set<String> selectedMapperTypes) {
        Map<String, Set<String>> mapperTypesByPackage = new TreeMap<>();
        for (String mapperType : selectedMapperTypes) {
            int lastDot = mapperType.lastIndexOf('.');
            String packageName = lastDot > 0 ? mapperType.substring(0, lastDot) : "";
            mapperTypesByPackage.computeIfAbsent(packageName, ignored -> new TreeSet<>()).add(mapperType);
        }

        for (Map.Entry<String, Set<String>> entry : mapperTypesByPackage.entrySet()) {
            String packageName = entry.getKey();
            String className = "MyBatisMapperScanRegistration_"
                + scan.elementName().replace('.', '_').replace('$', '_');
            String registrationName = packageName.isEmpty() ? className : packageName + "." + className;
            BYTE_CODE_GENERATOR.write(registrationDefinition(
                packageName,
                className,
                scan.elementName(),
                entry.getValue()
            ), context);
            context.visitServiceDescriptor(MyBatisMapperScanRegistration.class, registrationName, element);
        }
    }

    private ClassDef registrationDefinition(String packageName,
                                            String className,
                                            String customizerType,
                                            Set<String> mapperTypes) {
        String registrationName = packageName.isEmpty() ? className : packageName + "." + className;
        return ClassDef.builder(registrationName)
            .addModifiers(Modifier.FINAL)
            .addSuperinterface(ClassTypeDef.of(MyBatisMapperScanRegistration.class))
            .addMethod(MethodDef.builder("getCustomizerType")
                .overrides()
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .build((aThis, parameters) -> ExpressionDef.constant(customizerType).returning()))
            .addMethod(MethodDef.builder("register")
                .overrides()
                .addModifiers(Modifier.PUBLIC)
                .addParameter("configuration", Configuration.class)
                .build((aThis, parameters) -> StatementDef.multi(mapperTypes.stream()
                    .map(mapperType -> (StatementDef) parameters.get(0).invoke(
                        "addMapper",
                        TypeDef.VOID,
                        ExpressionDef.constant(ClassTypeDef.of(mapperType))
                    ))
                    .toList())))
            .build();
    }

    private boolean isInScannedPackage(String mapperType, Set<String> packages) {
        int lastDot = mapperType.lastIndexOf('.');
        String packageName = lastDot > 0 ? mapperType.substring(0, lastDot) : "";
        return packages.stream().anyMatch(scanPackage ->
            packageName.equals(scanPackage) || packageName.startsWith(scanPackage + "."));
    }

    record Scan(String elementName, String[] packages, String datasource) {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Scan(String otherElementName, String[] otherPackages, String otherDatasource))) {
                return false;
            }
            return elementName.equals(otherElementName)
                && Arrays.equals(packages, otherPackages)
                && datasource.equals(otherDatasource);
        }

        @Override
        public int hashCode() {
            int result = elementName.hashCode();
            result = 31 * result + Arrays.hashCode(packages);
            result = 31 * result + datasource.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "Scan[elementName=" + elementName
                + ", packages=" + Arrays.toString(packages)
                + ", datasource=" + datasource + "]";
        }
    }
}
