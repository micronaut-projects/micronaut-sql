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
package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.core.annotation.Internal;
import io.micronaut.jdbc.DelegatingDataSource;
import io.micronaut.jdbc.JdbcSqliteSupport;
import oracle.ucp.jdbc.PoolDataSource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;

/**
 * Creates SQLite-aware proxies for UCP datasources while preserving the {@link PoolDataSource} type.
 *
 * @since 7.0.0
 */
@Internal
final class UcpSqliteProxyFactory {

    private UcpSqliteProxyFactory() {
    }

    static PoolDataSource wrap(PoolDataSource dataSource, String driverClassName, String jdbcUrl) {
        if (!JdbcSqliteSupport.isSqlite(driverClassName, jdbcUrl) || dataSource instanceof DelegatingDataSource) {
            return dataSource;
        }
        return (PoolDataSource) Proxy.newProxyInstance(
                dataSource.getClass().getClassLoader(),
                new Class<?>[]{PoolDataSource.class, DelegatingDataSource.class},
                new PoolDataSourceInvocationHandler(dataSource)
        );
    }

    private static final class PoolDataSourceInvocationHandler implements InvocationHandler {

        private final PoolDataSource target;

        private PoolDataSourceInvocationHandler(PoolDataSource target) {
            this.target = target;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("getTargetDataSource".equals(methodName) && (args == null || args.length == 0)) {
                return target;
            }
            if ("getConnection".equals(methodName)) {
                return wrapConnection(method, args);
            }
            if ("unwrap".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> unwrapType) {
                if (unwrapType.isInstance(proxy)) {
                    return proxy;
                }
                if (unwrapType.isInstance(target)) {
                    return target;
                }
                return target.unwrap(unwrapType);
            }
            if ("isWrapperFor".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> unwrapType) {
                return unwrapType.isInstance(proxy) || unwrapType.isInstance(target) || target.isWrapperFor(unwrapType);
            }
            if ("equals".equals(methodName) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(methodName) && (args == null || args.length == 0)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(methodName) && (args == null || args.length == 0)) {
                return target.toString();
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private Connection wrapConnection(Method method, Object[] args) throws Throwable {
            try {
                return JdbcSqliteSupport.wrapSqliteConnection((Connection) method.invoke(target, args));
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }
}
