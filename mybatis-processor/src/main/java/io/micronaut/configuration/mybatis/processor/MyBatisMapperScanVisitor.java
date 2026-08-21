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
import io.micronaut.core.annotation.AnnotationClassValue;
import io.micronaut.core.annotation.AnnotationValue;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.visitor.TypeElementVisitor;
import io.micronaut.inject.visitor.VisitorContext;
import jakarta.inject.Named;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Adds compile-time discovered mapper types to {@link MyBatisMapperScan} metadata.
 */
public final class MyBatisMapperScanVisitor implements TypeElementVisitor<Object, Object> {

    private final Set<String> mapperTypes = new LinkedHashSet<>();
    private final Map<String, Scan> scans = new LinkedHashMap<>();
    private boolean processed;

    @Override
    public VisitorKind getVisitorKind() {
        return VisitorKind.AGGREGATING;
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

            AnnotationClassValue<?>[] mapperClassValues = selectedMapperTypes.stream()
                .map(AnnotationClassValue::new)
                .toArray(AnnotationClassValue[]::new);
            element.annotate(AnnotationValue.builder(MyBatisMapperScan.class)
                .member("value", scan.packages())
                .member("datasource", scan.datasource())
                .member("mappers", mapperClassValues)
                .build());
            element.annotate(Named.class, builder -> builder.value(scan.datasource()));
        }
    }

    private boolean isInScannedPackage(String mapperType, Set<String> packages) {
        int lastDot = mapperType.lastIndexOf('.');
        String packageName = lastDot > 0 ? mapperType.substring(0, lastDot) : "";
        return packages.stream().anyMatch(scanPackage ->
            packageName.equals(scanPackage) || packageName.startsWith(scanPackage + "."));
    }

    private record Scan(String elementName, String[] packages, String datasource) {

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof Scan scan)) {
                return false;
            }
            return elementName.equals(scan.elementName)
                && Arrays.equals(packages, scan.packages)
                && datasource.equals(scan.datasource);
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
