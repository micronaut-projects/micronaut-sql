/*
 * Copyright 2017-2025 original authors
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

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import oracle.ucp.jdbc.PoolDataSource;

/** Oracle UCP configuration setting some {@code oracle.ucp} system properties. */
@Context
@Internal
@Requires(classes = PoolDataSource.class)
@Requires(beans = OracleUcpConfiguration.class)
public class OracleUcpConfigurator {

    private static final String UCP_DESTROY_ON_RELOAD = "oracle.ucp.destroyOnReload";

    /**
     * Initializes the Oracle UCP (Universal Connection Pooling) configuration.
     *
     * <p>If the {@code oracle.ucp.destroyOnReload} system property is not set or is empty, this method
     * sets it based on the value of the {@link OracleUcpConfiguration#destroyOnReload()} field.
     * This property determines whether the connection pool should be destroyed when the application
     * is reloaded.
     *
     * <p>This initialization helps prevent duplicate connection pool errors that may occur during
     * application reloading.
     */
    @PostConstruct
    public void initUcp(@Nullable OracleUcpConfiguration oracleUcpConfiguration) {
        if (oracleUcpConfiguration != null && oracleUcpConfiguration.destroyOnReload() != null) {
            final String ucpDestroyOnReloadProperty = System.getProperty(UCP_DESTROY_ON_RELOAD);
            if (ucpDestroyOnReloadProperty == null || ucpDestroyOnReloadProperty.isBlank()) {
                // This is to deal with a duplicate connection pool error of:
                // "oracle.ucp.UniversalConnectionPoolException: Universal Connection Pool already
                // exists in the Universal Connection Pool Manager. Universal Connection Pool cannot
                // be added to the Universal Connection Pool Manager"
                // This magic flag is used by oracle.ucp.util.Util.isDestroyOnReloadEnabled, which
                // defaults it to false.
                // When true the oracle.ucp.jdbc.PoolDataSourceImpl.createUniversalConnectionPool()
                // will destroy the connection pool if it is still around.
                System.setProperty(
                    UCP_DESTROY_ON_RELOAD,
                    String.valueOf(oracleUcpConfiguration.destroyOnReload()));
            }
        }
    }
}
