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
package io.micronaut.configuration.jdbc.tomcat;

import io.micronaut.configuration.jdbc.tomcat.metadata.TomcatDataSourcePoolMetadata;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import io.micronaut.jdbc.JdbcSqliteSupport;
import io.micronaut.jdbc.OracleSessionProgramHelper;
import jakarta.annotation.PreDestroy;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Creates a tomcat data source for each configuration bean.
 *
 * @author James Kleeh
 * @author Christian Oestreich
 * @since 1.0
 */
@Factory
public class DatasourceFactory extends BaseDatasourceFactory implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private final Map<String, org.apache.tomcat.jdbc.pool.DataSource> dataSources = new LinkedHashMap<>(2);

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
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
    }

    /**
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @return An Apache Tomcat {@link DataSource}
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource(datasourceConfiguration);
        try {
            OracleSessionProgramHelper.apply(
                    datasourceConfiguration.getName(),
                    datasourceConfiguration.getUrl(),
                    applicationContext.getProperty("datasources." + datasourceConfiguration.getName() + ".dialect", String.class).orElse(null),
                    applicationContext.getEnvironment(),
                    (k, v) -> {
                        Properties p = ds.getDbProperties();
                        if (p == null) {
                            p = new Properties();
                            ds.setDbProperties(p);
                        }
                        p.setProperty(k, v);
                    },
                    () -> ds.getDbProperties() != null && ds.getDbProperties().containsKey("v$session.program")
            );
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping Oracle session program auto-config due to: {}", e.getMessage());
            }
        }
        dataSources.put(datasourceConfiguration.getName(), ds);
        return JdbcSqliteSupport.wrapDataSource(ds, ds.getDriverClassName(), ds.getUrl());
    }

    /**
     * Method to create a metadata object that allows pool value lookup for each datasource object.
     *
     * @param dataSource     The datasource
     * @return a {@link TomcatDataSourcePoolMetadata}
     */
    @EachBean(DataSource.class)
    @Requires(beans = {DatasourceConfiguration.class})
    public TomcatDataSourcePoolMetadata tomcatPoolDataSourceMetadataProvider(
            DataSource dataSource) {

        TomcatDataSourcePoolMetadata dataSourcePoolMetadata = null;

        if (dataSourceResolver.resolve(dataSource) instanceof org.apache.tomcat.jdbc.pool.DataSource resolved) {
            dataSourcePoolMetadata = new TomcatDataSourcePoolMetadata(resolved);
        }
        return dataSourcePoolMetadata;
    }

    @Override
    @PreDestroy
    public void close() {
        for (org.apache.tomcat.jdbc.pool.DataSource dataSource : dataSources.values()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Error closing data source [" + dataSource + "]: " + e.getMessage(), e);
                }
            }
        }
    }

    @Override
    protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        org.apache.tomcat.jdbc.pool.DataSource dataSource = dataSources.get(dataSourceName);
        if (dataSource != null) {
            if (dataSourceCredentials.password() != null) {
                dataSource.setPassword(dataSourceCredentials.password());
            }
            if (dataSourceCredentials.userName() != null) {
                dataSource.setUsername(dataSourceCredentials.userName());
            }
            dataSource.testIdle();
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
        }
    }
}
