/*
 * Copyright 2017-2020 original authors
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

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.core.annotation.Internal;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import org.jspecify.annotations.Nullable;

/**
 * The settings that choose between an XA and a plain pool, which {@link DatasourceConfiguration} must know before it is
 * bound to the pool, since it binds its other settings to that pool. They are a configuration bean of their own so
 * that {@link DatasourceConfiguration} receives no environment, which would keep development mode from retaining the
 * pool, and keeps binding through setters, which a refresh does in place rather than by closing the pool.
 *
 * @since 7.3.0
 */
@Internal
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
final class PoolTypeConfiguration {

    private boolean xa;
    private @Nullable String connectionFactoryClassName;
    private @Nullable String driverClassName;
    private @Nullable ClassLoader classLoader;
    private boolean developmentMode;

    /**
     * @param classLoader The class loader of the application context
     * @param developmentMode Whether development mode is on, in which the pool may be retained across a restart
     */
    void useEnvironmentSettings(ClassLoader classLoader, boolean developmentMode) {
        this.classLoader = classLoader;
        this.developmentMode = developmentMode;
    }

    /**
     * @return The class loader of the application context, or else the one of this module
     */
    ClassLoader classLoader() {
        return classLoader != null ? classLoader : PoolTypeConfiguration.class.getClassLoader();
    }

    /**
     * @return Whether development mode is on
     */
    boolean isDevelopmentMode() {
        return developmentMode;
    }

    boolean isXa() {
        return xa;
    }

    /**
     * @param xa Whether the pool is an XA pool
     */
    public void setXa(boolean xa) {
        this.xa = xa;
    }

    /**
     * @return The configured connection factory class, or else the driver class
     */
    @Nullable String factoryClassName() {
        return connectionFactoryClassName != null ? connectionFactoryClassName : driverClassName;
    }

    /**
     * @param connectionFactoryClassName The connection factory class
     */
    public void setConnectionFactoryClassName(@Nullable String connectionFactoryClassName) {
        this.connectionFactoryClassName = connectionFactoryClassName;
    }

    /**
     * @param driverClassName The driver class
     */
    public void setDriverClassName(@Nullable String driverClassName) {
        this.driverClassName = driverClassName;
    }
}
