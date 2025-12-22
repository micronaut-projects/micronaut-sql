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
package io.micronaut.jdbc;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates an unpooled datasource for each configuration bean.
 *
 * <p>This factory creates {@link UnpooledDataSource} instances that do not pool connections.
 * Each call to {@link DataSource#getConnection()} creates a new physical database connection,
 * and each call to close() actually closes the connection.</p>
 *
 * <p><strong>Warning:</strong> Unpooled datasources have significant performance implications
 * and should only be used for testing, serverless, or very low-volume applications.</p>
 *
 * <p>This factory is only activated when the property {@code datasources.allow-unpooled=true} 
 * is set globally, or {@code datasources.<name>.allow-unpooled=true} is set for a specific datasource.</p>
 *
 * @author Micronaut Team
 * @since 6.3.0
 */
@Factory
public class UnpooledDatasourceFactory extends BaseDatasourceFactory implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(UnpooledDatasourceFactory.class);
    private final Map<String, UnpooledDataSource> dataSources = new LinkedHashMap<>(2);

    /**
     * Default constructor.
     *
     * @param applicationContext The application context
     */
    public UnpooledDatasourceFactory(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * Creates an unpooled datasource for each {@link UnpooledDatasourceConfiguration}.
     *
     * @param datasourceConfiguration The datasource configuration
     * @return An unpooled {@link DataSource}
     */
    @Context
    @EachBean(UnpooledDatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    public DataSource dataSource(UnpooledDatasourceConfiguration datasourceConfiguration) {
        UnpooledDataSource ds = new UnpooledDataSource(
                datasourceConfiguration.getUrl(),
                datasourceConfiguration.getUsername(),
                datasourceConfiguration.getPassword(),
                datasourceConfiguration.getDriverClassName(),
                datasourceConfiguration.getDataSourcePropertiesAsProperties()
        );

        ds.setLoginTimeout(datasourceConfiguration.getLoginTimeout());
        dataSources.put(datasourceConfiguration.getName(), ds);

        if (LOG.isInfoEnabled()) {
            LOG.info("Created unpooled datasource [{}] for URL [{}]",
                    datasourceConfiguration.getName(),
                    datasourceConfiguration.getUrl());
        }

        return ds;
    }

    @Override
    protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        UnpooledDataSource unpooledDataSource = dataSources.get(dataSourceName);
        if (unpooledDataSource != null) {
            unpooledDataSource.updateCredentials(
                    dataSourceCredentials.userName(),
                    dataSourceCredentials.password()
            );
            if (LOG.isInfoEnabled()) {
                LOG.info("Updated credentials for unpooled datasource [{}]", dataSourceName);
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
        }
    }

    @Override
    @PreDestroy
    public void close() {
        // No cleanup needed for unpooled datasources
        // Connections are closed by the application when Connection.close() is called
        dataSources.clear();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Cleared unpooled datasource references");
        }
    }
}
