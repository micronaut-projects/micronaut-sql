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
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.NoSuchBeanException;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import io.micronaut.jdbc.JdbcSqliteSupport;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.jdbc.PoolDataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Creates an ucp data source for each configuration bean.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@Factory
public class DatasourceFactory extends BaseDatasourceFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private final UniversalConnectionPoolManagerConfiguration configuration;

    private final Map<String, PoolDataSource> dataSources = new LinkedHashMap<>(2);
    private UniversalConnectionPoolManager connectionPoolManager;
    private final DataSourceResolver dataSourceResolver;

    /**
     * Default constructor.
     *
     * @param dataSourceResolver The data source resolver
     * @param applicationContext The application context
     */
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver,
                             ApplicationContext applicationContext) {
        super(applicationContext);
        this.configuration = applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.class);
        try {
            this.connectionPoolManager = applicationContext.getBean(UniversalConnectionPoolManager.class);
        } catch (NoSuchBeanException e) {
            // no-op
        }
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
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
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        PoolDataSource ds = datasourceConfiguration.getPoolDataSource();
        dataSources.put(datasourceConfiguration.getName(), ds);

        return JdbcSqliteSupport.wrapDataSource(ds, datasourceConfiguration.getDriverClassName(), datasourceConfiguration.getUrl());
    }

    /**
     * Method to create a metadata object that allows pool value lookup for each datasource object.
     *
     * @param dataSource The actual datasource
     * @return a {@link OracleUcpDataSourcePoolMetadata}
     */
    @EachBean(DataSource.class)
    @Requires(beans = {DatasourceConfiguration.class})
    public OracleUcpDataSourcePoolMetadata ucpDataSourcePoolMetadata(DataSource dataSource) {
        OracleUcpDataSourcePoolMetadata ucpDataSourcePoolMetadata = null;

        if (dataSourceResolver.resolve(dataSource) instanceof PoolDataSource resolved) {
            ucpDataSourcePoolMetadata = new OracleUcpDataSourcePoolMetadata(resolved, connectionPoolManager);
        }
        return ucpDataSourcePoolMetadata;
    }

    @Override
    @PreDestroy
    public void close() {
        if (configuration.isEnabled() && connectionPoolManager != null) {
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
    protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        PoolDataSource dataSource = dataSources.get(dataSourceName);
        if (dataSource != null) {
            try {
                Properties props = new Properties();
                if (dataSourceCredentials.password() != null) {
                    props.put("password", dataSourceCredentials.password());
                }
                if (dataSourceCredentials.userName() != null) {
                    props.put("user", dataSourceCredentials.userName());
                }
                if (!props.isEmpty()) {
                    dataSource.reconfigureDataSource(props);
                }
            } catch (SQLException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to update username and/or password for datasource {}", dataSourceName, e);
                }
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
        }
    }
}
