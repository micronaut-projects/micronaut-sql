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
package io.micronaut.configuration.mybatis;

import io.micronaut.aop.InterceptorBean;
import io.micronaut.aop.MethodInterceptor;
import io.micronaut.aop.MethodInvocationContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.service.SoftServiceLoader;
import org.apache.ibatis.session.Configuration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Registers mapper types discovered by {@link MyBatisMapperScan}.
 */
@InterceptorBean(MyBatisMapperScan.class)
@Internal
final class MyBatisMapperScanInterceptor implements MethodInterceptor<Object, Object> {

    private final List<MyBatisMapperScanRegistration> registrations;

    MyBatisMapperScanInterceptor(List<MyBatisMapperScanRegistration> registrations) {
        this.registrations = new ArrayList<>(registrations);
        SoftServiceLoader.load(
            MyBatisMapperScanRegistration.class,
            MyBatisMapperScanInterceptor.class.getClassLoader()
        ).collectAll(this.registrations);
    }

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Object[] parameterValues = context.getParameterValues();
        if (parameterValues.length != 1 || !(parameterValues[0] instanceof Configuration configuration)) {
            return context.proceed();
        }
        registerMappers(context, configuration);
        return null;
    }

    private void registerMappers(MethodInvocationContext<Object, Object> context, Configuration configuration) {
        Class<?>[] mappers = context.classValues(MyBatisMapperScan.class, "mappers");
        if (mappers.length > 0) {
            registerMappers(configuration, mappers);
            return;
        }

        MapperScan mapperScan = findMapperScan(context.getTarget().getClass(), new HashSet<>());
        String customizerType = mapperScan == null
            ? context.getDeclaringType().getName()
            : mapperScan.type().getName();
        if (registerMappers(configuration, customizerType)) {
            return;
        }
        String[] packages = mapperScan == null
            ? context.stringValues(MyBatisMapperScan.class, "value")
            : mapperScan.annotation().value();
        registerMappers(configuration, packages);
    }

    private static void registerMappers(Configuration configuration, Class<?>[] mappers) {
        for (Class<?> mapper : mappers) {
            configuration.addMapper(mapper);
        }
    }

    private boolean registerMappers(Configuration configuration, String customizerType) {
        boolean registered = false;
        for (MyBatisMapperScanRegistration registration : registrations) {
            if (registration.getCustomizerType().equals(customizerType)) {
                registration.register(configuration);
                registered = true;
            }
        }
        return registered;
    }

    private static void registerMappers(Configuration configuration, String[] packages) {
        for (String packageName : packages) {
            configuration.addMappers(packageName);
        }
    }

    private static MapperScan findMapperScan(Class<?> type, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        MyBatisMapperScan mapperScan = type.getAnnotation(MyBatisMapperScan.class);
        if (mapperScan != null) {
            return new MapperScan(type, mapperScan);
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            MapperScan interfaceMapperScan = findMapperScan(interfaceType, visited);
            if (interfaceMapperScan != null) {
                return interfaceMapperScan;
            }
        }
        return findMapperScan(type.getSuperclass(), visited);
    }

    private record MapperScan(Class<?> type, MyBatisMapperScan annotation) {
    }
}
