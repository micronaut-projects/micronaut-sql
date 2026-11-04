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
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Retain;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.context.exceptions.DisabledBeanException;
import io.micronaut.context.event.BeanDestroyedEvent;
import io.micronaut.context.event.BeanDestroyedEventListener;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import io.micronaut.jdbc.OracleSessionProgramHelper;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolXADataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Creates an ucp data source for each configuration bean.
 * <p>
 * The factory holds neither the application context nor anything bound to it, so that development mode can retain
 * the factory with the pools it created across a restart, which {@link Retain} declares: a change of the
 * configuration under {@code datasources} or {@code ucp-manager}, or of the application name that the Oracle session
 * program comes from, releases them. The factory destroys its pools when it is destroyed.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@Factory
public class DatasourceFactory extends BaseDatasourceFactory implements AutoCloseable, BeanDestroyedEventListener<DataSource> {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private final boolean connectionPoolManagerEnabled;

    private final Map<String, PoolDataSource> dataSources = new LinkedHashMap<>(2);
    private final DataSourceResolver dataSourceResolver;

    /**
     * Default constructor.
     * <p>
     * The factory copies whether the connection pool manager is enabled from its configuration, rather than holding the
     * configuration or the manager bean, whose implementations resolve through the application context: development
     * mode binds the configuration again for each restart, and a change under {@value UniversalConnectionPoolManagerConfiguration#PREFIX}
     * releases the factory. The manager is the one of the JVM,
     * {@link UniversalConnectionPoolManagerImpl#getUniversalConnectionPoolManager()}, which the manager bean also is.
     *
     * @param dataSourceResolver The data source resolver
     * @param configuration The configuration of the connection pool manager
     * @since 7.3.0
     */
    @Inject
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver,
                             UniversalConnectionPoolManagerConfiguration configuration) {
        super();
        this.connectionPoolManagerEnabled = configuration.isEnabled();
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
    }

    /**
     * Constructor reading the configuration of the connection pool manager from the application context. The context
     * is kept only in the deprecated {@link BaseDatasourceFactory#applicationContext} field, for subclasses compiled
     * against an earlier release.
     *
     * @param dataSourceResolver The data source resolver
     * @param applicationContext The application context
     * @deprecated The factory receives what it needs rather than the application context, so that its pools can
     * outlive it. Use {@link #DatasourceFactory(DataSourceResolver, UniversalConnectionPoolManagerConfiguration)}.
     */
    @Deprecated(since = "7.3.0", forRemoval = true)
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver,
                             ApplicationContext applicationContext) {
        super(applicationContext);
        this.connectionPoolManagerEnabled = applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.class).isEnabled()
                && applicationContext.containsBean(UniversalConnectionPoolManager.class);
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
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
     * @deprecated Use {@link #dataSource(DatasourceConfiguration, List)} instead.
     */
    @Deprecated(since = "7.3.0", forRemoval = true)
    public PoolDataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        return dataSource(datasourceConfiguration, List.of());
    }

    /**
     * Method to get a PoolDataSource from the {@link DatasourceConfiguration}.
     *
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @param customizers The pool data source customizers, applied in order before the pool starts
     * @return A {@link PoolDataSource}
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    @Retain(invalidatedBy = {BasicJdbcConfiguration.PREFIX, OracleSessionProgramHelper.APPLICATION_PREFIX, UniversalConnectionPoolManagerConfiguration.PREFIX})
    public PoolDataSource dataSource(DatasourceConfiguration datasourceConfiguration,
                                     List<PoolDataSourceCustomizer> customizers) {
        PoolDataSource ds = datasourceConfiguration.getPoolDataSource();
        for (PoolDataSourceCustomizer customizer : customizers) {
            try {
                customizer.customize(datasourceConfiguration.getName(), ds);
            } catch (SQLException e) {
                throw new ConfigurationException("Failed to customize data source [" + datasourceConfiguration.getName() + "]: " + e.getMessage(), e);
            }
        }
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

    /**
     * Exposes the pool of a data source with {@link DatasourceConfiguration#setXa(boolean) xa} enabled as an
     * {@link XADataSource} for transaction managers that enlist XA connections. The bean delegates to the
     * {@link PoolXADataSource} of the data source bean, which development mode may have retained from an earlier
     * context, and is not itself a {@link DataSource}, so the pool remains the only {@link DataSource} bean of its
     * name. That bean still opens connections for local transactions, from XA connections of the same pool.
     *
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @param dataSource The data source bean of the configuration, required so that the pool is created, customized and started first
     * @return The XA data source; the bean is disabled when xa is not enabled
     * @since 7.3.0
     */
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    public XADataSource xaDataSource(DatasourceConfiguration datasourceConfiguration,
                                     @Parameter DataSource dataSource) {
        if (!datasourceConfiguration.isXa()) {
            throw new DisabledBeanException("Data source [" + datasourceConfiguration.getName() + "] does not have xa enabled");
        }
        PoolXADataSource pool = dataSourceResolver.resolve(dataSource) instanceof PoolXADataSource resolved
            ? resolved
            : (PoolXADataSource) datasourceConfiguration.getPoolDataSource();
        return new UcpXADataSource(pool);
    }

    /**
     * Closes the pool of a data source once its bean is destroyed, after the beans that depend on it, such as a
     * JPA {@code SessionFactory} that drops the schema on close. The remaining pools are closed by {@link #close()}.
     *
     * @param event The bean destroyed event
     */
    @Override
    public void onDestroyed(BeanDestroyedEvent<DataSource> event) {
        String name = findDataSourceName(event);
        PoolDataSource dataSource = name == null ? null : dataSources.remove(name);
        if (dataSource != null) {
            close(dataSource);
        }
    }

    @Override
    @PreDestroy
    public void close() {
        for (PoolDataSource dataSource : dataSources.values()) {
            close(dataSource);
        }
        dataSources.clear();
    }

    private void close(PoolDataSource dataSource) {
        UniversalConnectionPoolManager connectionPoolManager = connectionPoolManager();
        if (connectionPoolManager != null) {
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
