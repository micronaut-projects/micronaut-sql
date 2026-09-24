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
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shared logic of the {@link MyBatisMapperScan} visitors.
 */
@Internal
final class MapperScanSupport {

    private MapperScanSupport() {
    }

    /**
     * Reads the members of a {@link MyBatisMapperScan} annotation.
     *
     * @param element The annotated element
     * @return The scan, or {@code null} if the element is not annotated
     */
    static Scan readScan(ClassElement element) {
        AnnotationValue<MyBatisMapperScan> annotation = element.getAnnotation(MyBatisMapperScan.class);
        if (annotation == null) {
            return null;
        }
        List<String> mappers = new ArrayList<>();
        for (AnnotationClassValue<?> mapper : annotation.annotationClassValues("mappers")) {
            mappers.add(mapper.getName());
        }
        return new Scan(
            element,
            List.of(annotation.stringValues("value")),
            mappers,
            annotation.stringValue("datasource").orElse("default"),
            annotation.booleanValue("nativeImageMetadata").orElse(true)
        );
    }

    /**
     * Collects the element and its nested types when they are interfaces. Nested interfaces are included
     * because MyBatis runtime package scanning registers them as well.
     *
     * @param element    The element
     * @param interfaces The collected interfaces
     */
    static void collectInterfaces(ClassElement element, List<ClassElement> interfaces) {
        if (element.isInterface() && !element.isAssignable(Annotation.class)) {
            interfaces.add(element);
        }
        for (ClassElement inner : element.getEnclosedElements(ElementQuery.ALL_INNER_CLASSES)) {
            collectInterfaces(inner, interfaces);
        }
    }

    /**
     * @param typeName    A type name
     * @param packageName A package name
     * @return Whether the type is in the package or one of its sub-packages
     */
    static boolean isInPackage(String typeName, String packageName) {
        int lastDot = typeName.lastIndexOf('.');
        String typePackage = lastDot > 0 ? typeName.substring(0, lastDot) : "";
        return typePackage.equals(packageName) || typePackage.startsWith(packageName + ".");
    }

    /**
     * Collects the result and parameter types of the mapper methods, unwrapping containers. These are the
     * types MyBatis instantiates and populates reflectively.
     *
     * @param mapper          The mapper interface
     * @param reflectiveTypes The collected type names
     */
    static void collectReflectiveTypes(ClassElement mapper, Set<String> reflectiveTypes) {
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
     * The members of a {@link MyBatisMapperScan} annotation.
     *
     * @param element             The annotated element
     * @param packages            The scanned packages
     * @param mappers             The explicitly listed mappers
     * @param datasource          The datasource name
     * @param nativeImageMetadata Whether to generate GraalVM metadata
     */
    record Scan(ClassElement element,
                List<String> packages,
                List<String> mappers,
                String datasource,
                boolean nativeImageMetadata) {

        boolean covers(String typeName) {
            return packages.stream().anyMatch(packageName -> isInPackage(typeName, packageName));
        }
    }
}
