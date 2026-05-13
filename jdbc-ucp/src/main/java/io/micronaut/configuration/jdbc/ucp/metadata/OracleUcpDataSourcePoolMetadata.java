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
package io.micronaut.configuration.jdbc.ucp.metadata;

import io.micronaut.jdbc.metadata.AbstractDataSourcePoolMetadata;
import oracle.ucp.UniversalConnectionPool;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.jdbc.PoolDataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * {@link io.micronaut.jdbc.metadata.DataSourcePoolMetadata} for an Oracle UCP {@link PoolDataSource}.
 *
 * @author Andreas Brenk
 * @since 7.0.0
 */
public class OracleUcpDataSourcePoolMetadata
    extends AbstractDataSourcePoolMetadata<PoolDataSource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OracleUcpDataSourcePoolMetadata.class);

    private final UniversalConnectionPoolManager connectionPoolManager;

    /**
     * Oracle UCP typed {@link io.micronaut.jdbc.metadata.DataSourcePoolMetadata} object.
     *
     * @param dataSource The datasource
     * @param connectionPoolManager The connection pool manager
     */
    public OracleUcpDataSourcePoolMetadata(PoolDataSource dataSource, UniversalConnectionPoolManager connectionPoolManager) {
        super(dataSource);
        this.connectionPoolManager = connectionPoolManager;
    }

    @Override
    public @Nullable Integer getIdle() {
        return getConnectionPool()
            .map(UniversalConnectionPool::getAvailableConnectionsCount)
            .orElse(null);
    }

    @Override
    public @Nullable Integer getActive() {
        return getConnectionPool()
            .map(UniversalConnectionPool::getBorrowedConnectionsCount)
            .orElse(null);
    }

    @Override
    public @Nullable Integer getMax() {
        return getConnectionPool()
            .map(UniversalConnectionPool::getMaxPoolSize)
            .orElse(null);
    }

    @Override
    public @Nullable Integer getMin() {
        return getConnectionPool()
            .map(UniversalConnectionPool::getMinPoolSize)
            .orElse(null);
    }

    @Override
    public @Nullable String getValidationQuery() {
        return getDataSource().getSQLForValidateConnection();
    }

    @Override
    public Boolean getDefaultAutoCommit() {
        return getDataSource().isCommitOnConnectionReturn();
    }

    /**
     * Get the {@link UniversalConnectionPool} from the {@link UniversalConnectionPoolManager}.
     */
    private Optional<UniversalConnectionPool> getConnectionPool() {
        String poolName = getDataSource().getConnectionPoolName();

        try {
            return Optional.ofNullable(connectionPoolManager.getConnectionPool(poolName));
        } catch (UniversalConnectionPoolException e) {
            LOGGER.error("Could not get connection pool from connection pool manager", e);
        }

        return Optional.empty();
    }

}
