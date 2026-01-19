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
import org.jspecify.annotations.Nullable;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

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
     * @param dataSourceResolver The data source resolver
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
        try {
            applyOracleSessionProgram(datasourceConfiguration);
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping Oracle session program auto-config due to: {}", e.getMessage());
            }
        }
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource(datasourceConfiguration);
        dataSources.put(datasourceConfiguration.getName(), ds);
        return ds;
    }

    private void applyOracleSessionProgram(DatasourceConfiguration cfg) {
        String url = cfg.getUrl();
        if (url == null) {
            return;
        }
        String lower = url.toLowerCase();
        if (!lower.startsWith("jdbc:oracle")) {
            return;
        }
        String dsName = cfg.getName();
        boolean enabled = applicationContext.getProperty("datasources." + dsName + ".oracle.session.enabled", boolean.class).orElse(true);
        if (!enabled) {
            return;
        }
        java.util.Properties props = cfg.getDbProperties();
        if (props != null && props.containsKey("v$session.program")) {
            return;
        }
        String program = applicationContext.getProperty("datasources." + dsName + ".oracle.session.program", String.class)
            .orElseGet(() -> applicationContext.getProperty("micronaut.application.name", String.class).orElse("Micronaut"));
        if (props == null) {
            props = new java.util.Properties();
            cfg.setDbProperties(props);
        }
        props.put("v$session.program", program);
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
