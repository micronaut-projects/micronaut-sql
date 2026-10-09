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
package io.micronaut.configuration.jdbc.hikari;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Retain;
import io.micronaut.jdbc.BaseDatasourceFactory;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.JdbcDataSourceEnabled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Creates a Hikari data source for each configuration bean.
 * <p>
 * The factory holds neither the application context nor anything bound to it, so that development mode can retain
 * the factory with the pools it created across a restart, which {@link Retain} declares: a change of the
 * configuration under {@code datasources} releases them. The factory closes its pools when it is destroyed. The
 * Oracle session program and the metrics, which need the context, are applied by bean created event listeners:
 * {@code OracleSessionProgramConfigurer} and {@code HikariMetricsConfigurer}.
 *
 * @author James Kleeh
 * @author Christian Oestreich
 * @since 1.0
 */
@Factory
public class DatasourceFactory extends BaseDatasourceFactory implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private final Map<String, HikariUrlDataSource> dataSources = new LinkedHashMap<>(2);

    /**
     * Default constructor.
     *
     * @since 7.3.0
     */
    @Inject
    public DatasourceFactory() {
    }

    /**
     * Constructor taking the application context, which the factory no longer uses. It is kept in the deprecated
     * {@link BaseDatasourceFactory#applicationContext} field for subclasses compiled against an earlier release.
     *
     * @param applicationContext The application context
     * @deprecated The factory holds no application context, so that its pools can outlive it. Use {@link #DatasourceFactory()}.
     */
    @Deprecated(since = "7.3.0", forRemoval = true)
    public DatasourceFactory(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    /**
     * Method to wire up all the HikariCP connections based on the {@link DatasourceConfiguration}.
     * If a {@code MeterRegistry} bean exists then the registry will be added to the datasource, by {@code HikariMetricsConfigurer}.
     *
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @return A {@link HikariUrlDataSource}
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    @Requires(condition = JdbcDataSourceEnabled.class)
    @Retain(invalidatedBy = BasicJdbcConfiguration.PREFIX)
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        HikariUrlDataSource ds = new HikariUrlDataSource(datasourceConfiguration);
        dataSources.put(datasourceConfiguration.getName(), ds);
        return ds;
    }

    @Override
    protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
        HikariUrlDataSource hikariUrlDataSource = dataSources.get(dataSourceName);
        if (hikariUrlDataSource != null) {
            if (dataSourceCredentials.userName() != null) {
                hikariUrlDataSource.setUsername(dataSourceCredentials.userName());
                hikariUrlDataSource.getHikariConfigMXBean().setUsername(dataSourceCredentials.userName());
            }
            if (dataSourceCredentials.password() != null) {
                hikariUrlDataSource.setPassword(dataSourceCredentials.password());
                hikariUrlDataSource.getHikariConfigMXBean().setPassword(dataSourceCredentials.password());
            }
            hikariUrlDataSource.getHikariPoolMXBean().softEvictConnections();
        } else if (LOG.isDebugEnabled()) {
            LOG.debug("Datasource with name [{}] not found while trying to propagate datasource credentials changes.", dataSourceName);
        }
    }

    @Override
    @PreDestroy
    public void close() {
        for (HikariUrlDataSource dataSource : dataSources.values()) {
            try {
                dataSource.close();
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Error closing data source [" + dataSource + "]: " + e.getMessage(), e);
                }
            }
        }
    }
}
