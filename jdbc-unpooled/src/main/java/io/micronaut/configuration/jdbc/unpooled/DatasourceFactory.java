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
package io.micronaut.configuration.jdbc.unpooled;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import io.micronaut.jdbc.JdbcSqliteSupport;
import io.micronaut.jdbc.OracleSessionProgramHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Creates an unpooled datasource for each datasource configuration.
 *
 * @author Micronaut
 * @since 7.0.0
 */
@Factory
public class DatasourceFactory extends BaseDatasourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);

    private final Map<String, DriverManagerDataSource> dataSources = new ConcurrentHashMap<>(2);

    /**
     * @param applicationContext The application context
     */
    public DatasourceFactory(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * @param datasourceConfiguration The datasource configuration
     * @return An unpooled datasource
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(datasourceConfiguration);
        try {
            OracleSessionProgramHelper.apply(
                    datasourceConfiguration.getName(),
                    datasourceConfiguration.getUrl(),
                    applicationContext.getProperty("datasources." + datasourceConfiguration.getName() + ".dialect", String.class).orElse(null),
                    applicationContext.getEnvironment(),
                    dataSource::addDataSourceProperty,
                    () -> dataSource.hasDataSourceProperty("v$session.program")
            );
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping Oracle session program auto-config due to: {}", e.getMessage());
            }
        }
        dataSources.put(datasourceConfiguration.getName(), dataSource);
        return JdbcSqliteSupport.wrapDataSource(dataSource, dataSource.getDriverClassName(), dataSource.getUrl());
    }

    @Override
    protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        DriverManagerDataSource dataSource = dataSources.get(dataSourceName);
        if (dataSource != null) {
            if (dataSourceCredentials.userName() != null) {
                dataSource.setUsername(dataSourceCredentials.userName());
            }
            if (dataSourceCredentials.password() != null) {
                dataSource.setPassword(dataSourceCredentials.password());
            }
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
        }
    }
}
