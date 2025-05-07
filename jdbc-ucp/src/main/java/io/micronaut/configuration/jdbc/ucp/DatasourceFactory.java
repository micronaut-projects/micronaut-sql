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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.context.exceptions.NoSuchBeanException;
import io.micronaut.jdbc.DataSourcePasswordChangedEvent;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.jdbc.PoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates an ucp data source for each configuration bean.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@Factory
public class DatasourceFactory implements AutoCloseable, ApplicationEventListener<DataSourcePasswordChangedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private final ApplicationContext applicationContext;
    private final UniversalConnectionPoolManagerConfiguration configuration;

    private Map<String, PoolDataSource> dataSources = new HashMap<>(2);
    private UniversalConnectionPoolManager connectionPoolManager;

    /**
     * Default constructor.
     *
     * @param applicationContext The application context
     */
    public DatasourceFactory(ApplicationContext applicationContext) throws UniversalConnectionPoolException {
        this.applicationContext = applicationContext;
        this.configuration = applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.class);
        try {
            this.connectionPoolManager = applicationContext.getBean(UniversalConnectionPoolManager.class);
        } catch (NoSuchBeanException e) {
            // no-op
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
    public PoolDataSource dataSource(DatasourceConfiguration datasourceConfiguration) throws UniversalConnectionPoolException {
        PoolDataSource ds = datasourceConfiguration.getPoolDataSource();
        dataSources.put(datasourceConfiguration.getName(), ds);

        return ds;
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
    public void onApplicationEvent(DataSourcePasswordChangedEvent event) {
        DataSourcePasswordChangedEvent.DataSourcePasswordModel dataSourcePasswordModel = event.getDataSourcePasswordModel();
        String dataSourceName = dataSourcePasswordModel.dataSourceName();
        PoolDataSource dataSource = dataSources.get(dataSourceName);
        if (dataSource != null) {
            try {
                dataSource.setPassword(dataSourcePasswordModel.newPassword());
            } catch (SQLException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to update password for datasource {}", dataSourceName, e);
                }
            }
        }
    }
}
