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
import org.apache.ibatis.session.Configuration;

import java.util.HashSet;
import java.util.Set;

/**
 * Registers mapper types discovered by {@link MyBatisMapperScan}.
 */
@InterceptorBean(MyBatisMapperScan.class)
@Internal
final class MyBatisMapperScanInterceptor implements MethodInterceptor<Object, Object> {

    @Override
    public Object intercept(MethodInvocationContext<Object, Object> context) {
        Object[] parameterValues = context.getParameterValues();
        if (parameterValues.length == 1 && parameterValues[0] instanceof Configuration configuration) {
            Class<?>[] mappers = context.classValues(MyBatisMapperScan.class, "mappers");
            if (mappers.length > 0) {
                for (Class<?> mapper : mappers) {
                    configuration.addMapper(mapper);
                }
            } else {
                MyBatisMapperScan mapperScan = findMapperScan(context.getTarget().getClass(), new HashSet<>());
                String[] packages = mapperScan == null
                    ? context.stringValues(MyBatisMapperScan.class, "value")
                    : mapperScan.value();
                for (String packageName : packages) {
                    configuration.addMappers(packageName);
                }
            }
        }
        return null;
    }

    private static MyBatisMapperScan findMapperScan(Class<?> type, Set<Class<?>> visited) {
        if (type == null || !visited.add(type)) {
            return null;
        }
        MyBatisMapperScan mapperScan = type.getAnnotation(MyBatisMapperScan.class);
        if (mapperScan != null) {
            return mapperScan;
        }
        for (Class<?> interfaceType : type.getInterfaces()) {
            mapperScan = findMapperScan(interfaceType, visited);
            if (mapperScan != null) {
                return mapperScan;
            }
        }
        return findMapperScan(type.getSuperclass(), visited);
    }
}
