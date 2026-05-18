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
package io.micronaut.configuration.jdbc.dbcp;

import io.micronaut.configuration.jdbc.dbcp.metadata.DbcpDataSourcePoolMetadata;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import io.micronaut.jdbc.metadata.DataSourcePoolMetadata;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a dbcp data source for each configuration bean.
 *
 * @author Christian Oestreich
 * @since 1.0
 */
@Factory
public class DatasourceFactory extends BaseDatasourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);

    private final Map<String, BasicDataSource> dataSources = new LinkedHashMap<>(2);
    private final DataSourceResolver dataSourceResolver;

    /**
     * Default constructor.
     * @param dataSourceResolver The data source resolver
     * @param applicationContext The application context
     */
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver,
                             ApplicationContext applicationContext) {
        super(applicationContext);
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
    }

    /**
     * Re-exposes the DBCP configuration bean as the actual runtime datasource bean.
     *
     * @param datasourceConfiguration The datasource configuration
     * @return The DBCP datasource
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    public BasicDataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        BasicDataSource basicDataSource = datasourceConfiguration.getBasicDataSource();
        dataSources.put(datasourceConfiguration.getName(), basicDataSource);
        return basicDataSource;
    }

    /**
     * Method to create a metadata object that allows pool value lookup for each datasource object.
     *
     * @param dataSource The actual datasource
     * @return a {@link io.micronaut.jdbc.metadata.DataSourcePoolMetadata}
     */
    @EachBean(DataSource.class)
    public @Nullable DataSourcePoolMetadata<BasicDataSource> dbcpDataSourcePoolMetadata(
            DataSource dataSource) {
        DbcpDataSourcePoolMetadata dbcpDataSourcePoolMetadata = null;
        DataSource resolved = dataSourceResolver.resolve(dataSource);

        if (resolved instanceof BasicDataSource basicDataSource) {
            dbcpDataSourcePoolMetadata = new DbcpDataSourcePoolMetadata(basicDataSource);
        }
        return dbcpDataSourcePoolMetadata;
    }

    @Override
    protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        BasicDataSource basicDataSource = dataSources.get(dataSourceName);
        if (basicDataSource == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
            }
            return;
        }

        if (dataSourceCredentials.userName() != null) {
            basicDataSource.setUsername(dataSourceCredentials.userName());
        }
        if (dataSourceCredentials.password() != null) {
            basicDataSource.setPassword(dataSourceCredentials.password());
        }
        try {
            basicDataSource.restart();
        } catch (SQLException e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Failed to restart datasource after password change {}", dataSourceName, e);
            }
        }
    }
}
