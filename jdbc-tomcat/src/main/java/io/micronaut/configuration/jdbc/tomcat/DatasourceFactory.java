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
import io.micronaut.context.annotation.Retain;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a tomcat data source for each configuration bean.
 * <p>
 * The factory holds neither the application context nor anything bound to it, so that development mode can retain
 * the factory with the pools it created across a restart, which {@link Retain} declares: a change of the
 * configuration under {@code datasources} releases them. The factory closes its pools when it is destroyed. The
 * Oracle session program, which needs the environment, is applied by a bean created event listener,
 * {@code OracleSessionProgramConfigurer}.
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
     * @since 7.3.0
     */
    @Inject
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver) {
        super();
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
    }

    /**
     * Constructor taking the application context, which the factory no longer uses. It is kept in the deprecated
     * {@link BaseDatasourceFactory#applicationContext} field for subclasses compiled against an earlier release.
     *
     * @param dataSourceResolver The data source resolver
     * @param applicationContext The application context
     * @deprecated The factory holds no application context, so that its pools can outlive it. Use {@link #DatasourceFactory(DataSourceResolver)}.
     */
    @Deprecated(since = "7.3.0", forRemoval = true)
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
    @Retain(invalidatedBy = BasicJdbcConfiguration.PREFIX)
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        org.apache.tomcat.jdbc.pool.DataSource ds = new org.apache.tomcat.jdbc.pool.DataSource(datasourceConfiguration);
        dataSources.put(datasourceConfiguration.getName(), ds);
        return ds;
    }

    /**
     * Method to create a metadata object that allows pool value lookup for each datasource object.
     *
     * @param dataSource     The datasource
     * @return a {@link TomcatDataSourcePoolMetadata}
     */
    @EachBean(DataSource.class)
    @Requires(beans = {DatasourceConfiguration.class})
    public @Nullable TomcatDataSourcePoolMetadata tomcatPoolDataSourceMetadataProvider(
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
            // soft eviction: idle connections are closed now, connections in use are closed when returned to the pool
            dataSource.purge();
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
        }
    }
}
