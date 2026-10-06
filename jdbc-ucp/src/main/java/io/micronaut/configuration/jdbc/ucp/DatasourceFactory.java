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

import io.micronaut.configuration.jdbc.ucp.metadata.OracleUcpDataSourcePoolMetadata;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Retain;
import io.micronaut.core.util.StringUtils;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Creates an ucp data source for each configuration bean.
 * <p>
 * The factory holds neither the application context nor anything bound to it, so that development mode can retain
 * the factory with the pools it created across a restart, which {@link Retain} declares: a change of the
 * configuration under {@code datasources} or {@code ucp-manager} releases them. The factory destroys its pools when
 * it is destroyed.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@Factory
public class DatasourceFactory extends BaseDatasourceFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private final boolean connectionPoolManagerEnabled;

    private final Map<String, PoolDataSource> dataSources = new LinkedHashMap<>(2);
    private final DataSourceResolver dataSourceResolver;

    /**
     * Default constructor.
     * <p>
     * The factory receives whether the connection pool manager is enabled, rather than the manager bean or its
     * configuration, whose implementations resolve through the application context. The manager is the one of the
     * JVM, {@link UniversalConnectionPoolManagerImpl#getUniversalConnectionPoolManager()}, which the manager bean
     * also is.
     *
     * @param dataSourceResolver The data source resolver
     * @param connectionPoolManagerEnabled Whether the connection pool manager is enabled, {@code ucp-manager.enabled}
     * @since 7.3.0
     */
    @Inject
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver,
                             @Property(name = UniversalConnectionPoolManagerConfiguration.PREFIX + ".enabled", defaultValue = StringUtils.TRUE)
                             boolean connectionPoolManagerEnabled) {
        this.connectionPoolManagerEnabled = connectionPoolManagerEnabled;
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
    }

    /**
     * Constructor reading the configuration of the connection pool manager from the application context, which the
     * factory does not keep.
     *
     * @param dataSourceResolver The data source resolver
     * @param applicationContext The application context
     * @deprecated The factory receives what it needs rather than the application context, so that its pools can
     * outlive it. Use {@link #DatasourceFactory(DataSourceResolver, boolean)}.
     */
    @Deprecated(since = "7.3.0", forRemoval = true)
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver,
                             ApplicationContext applicationContext) {
        this(dataSourceResolver,
                applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.class).isEnabled()
                        && applicationContext.containsBean(UniversalConnectionPoolManager.class));
    }

    /**
     * @return The connection pool manager, or null when it is disabled or unavailable
     */
    private @Nullable UniversalConnectionPoolManager connectionPoolManager() {
        if (!connectionPoolManagerEnabled) {
            return null;
        }
        try {
            return UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
        } catch (UniversalConnectionPoolException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Unable to obtain the Universal Connection Pool Manager: " + e.getMessage(), e);
            }
            return null;
        }
    }

    /**
     * Method to get a PoolDataSource from the {@link DatasourceConfiguration}.
     *
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @return A {@link PoolDataSource}
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    @Retain(invalidatedBy = {BasicJdbcConfiguration.PREFIX, UniversalConnectionPoolManagerConfiguration.PREFIX})
    public PoolDataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        PoolDataSource ds = datasourceConfiguration.getPoolDataSource();
        dataSources.put(datasourceConfiguration.getName(), ds);

        return ds;
    }

    /**
     * Method to create a metadata object that allows pool value lookup for each datasource object.
     *
     * @param dataSource The actual datasource
     * @return a {@link OracleUcpDataSourcePoolMetadata}
     */
    @EachBean(DataSource.class)
    @Requires(beans = {DatasourceConfiguration.class})
    public @Nullable OracleUcpDataSourcePoolMetadata ucpDataSourcePoolMetadata(DataSource dataSource) {
        OracleUcpDataSourcePoolMetadata ucpDataSourcePoolMetadata = null;

        if (dataSourceResolver.resolve(dataSource) instanceof PoolDataSource resolved) {
            ucpDataSourcePoolMetadata = new OracleUcpDataSourcePoolMetadata(resolved, connectionPoolManager());
        }
        return ucpDataSourcePoolMetadata;
    }

    @Override
    @PreDestroy
    public void close() {
        UniversalConnectionPoolManager connectionPoolManager = connectionPoolManager();
        if (connectionPoolManager != null) {
            for (PoolDataSource dataSource : dataSources.values()) {
                try {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Closing connection pool named: {}", dataSource.getConnectionPoolName());
                    }
                    connectionPoolManager.destroyConnectionPool(dataSource.getConnectionPoolName());
                } catch (Exception e) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Error closing data source [" + dataSource + "]: " + e.getMessage(), e);
                    }
                }
            }
        }
    }

    @Override
    protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) throws SQLException {
        PoolDataSource dataSource = dataSources.get(dataSourceName);
        if (dataSource != null) {
            Properties props = new Properties();
            if (dataSourceCredentials.password() != null) {
                props.put("password", dataSourceCredentials.password());
            }
            if (dataSourceCredentials.userName() != null) {
                props.put("user", dataSourceCredentials.userName());
            }
            if (!props.isEmpty()) {
                // a failure is logged by the caller, and the change is retried by a later refresh event
                dataSource.reconfigureDataSource(props);
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
        }
    }
}
