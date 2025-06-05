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
    private static final String UCP_CREATE_CONNECTION_IN_BORROW_THREAD = "oracle.ucp.createConnectionInBorrowThread";

    /**
     * Initializes Oracle Universal Connection Pooling (UCP) configuration based on the provided
     * {@link OracleUcpConfiguration}. This method sets system properties for UCP configuration
     * if they are not already set.
     *
     * Specifically, it sets the following system properties:
     * <ul>
     *   <li>{@code oracle.ucp.destroyOnReload}: Controls whether to destroy connections on reload.
     *   <li>{@code oracle.ucp.createConnectionInBorrowThread}: Controls whether the connection pool
     *       should create connections in the borrow thread.
     * </ul>
     *
     * These properties are only set if the corresponding values in the provided configuration are not null
     * and the system properties are not already set or are blank.
     *
     * @param oracleUcpConfiguration the UCP configuration to apply, or null if no configuration is available
     */
    @PostConstruct
    public void initUcp(@Nullable OracleUcpConfiguration oracleUcpConfiguration) {
        if (oracleUcpConfiguration == null) {
            return;
        }

        if (oracleUcpConfiguration.destroyOnReload() != null) {
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

        if (oracleUcpConfiguration.createConnectionInBorrowThread() != null) {
            final String ucpCreateConnectionInBorrowThread = System.getProperty(UCP_CREATE_CONNECTION_IN_BORROW_THREAD);
            if (ucpCreateConnectionInBorrowThread == null || ucpCreateConnectionInBorrowThread.isBlank()) {
                // The current default behavior in UCP 23.x is to use background threads for creating connections,
                // instead of the user threads, which results in enhanced efficiency. If required,
                // it can be switched back to the old behavior by setting system property oracle.ucp.createConnectionInBorrowThread to true
                System.setProperty(
                    UCP_CREATE_CONNECTION_IN_BORROW_THREAD,
                    String.valueOf(oracleUcpConfiguration.createConnectionInBorrowThread()));
            }
        }
    }
}
