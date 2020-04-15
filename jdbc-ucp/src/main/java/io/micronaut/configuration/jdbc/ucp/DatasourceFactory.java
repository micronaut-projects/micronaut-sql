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

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Creates a UCP data source for each configuration bean.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@Factory
public class DatasourceFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private List<PoolDataSource> dataSources = new ArrayList<>(2);

    /**
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @return A UCP {@link PoolDataSource}
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    public PoolDataSource dataSource(DatasourceConfiguration datasourceConfiguration) throws SQLException {
        PoolDataSource ds = PoolDataSourceFactory.getPoolDataSource();
        ds.setConnectionFactoryClassName(datasourceConfiguration.connectionFactoryClassName);
        ds.setURL(datasourceConfiguration.url);
        ds.setUser(datasourceConfiguration.username);
        ds.setPassword(datasourceConfiguration.password);
        ds.setConnectionPoolName(datasourceConfiguration.connectionPoolName);
        ds.setInitialPoolSize(datasourceConfiguration.initialPoolSize);
        ds.setMinPoolSize(datasourceConfiguration.minPoolSize);
        ds.setMaxPoolSize(datasourceConfiguration.maxPoolSize);
        ds.setTimeoutCheckInterval(datasourceConfiguration.timeoutCheckInterval);
        ds.setInactiveConnectionTimeout(datasourceConfiguration.inactiveConnectionTimeout);
        Properties props = new Properties();
        props.setProperty("fixedString", String.valueOf(datasourceConfiguration.fixedString));
        props.setProperty("remarksReporting", String.valueOf(datasourceConfiguration.remarksReporting));
        props.setProperty("restrictGetTables", String.valueOf(datasourceConfiguration.restrictGetTables));
        props.setProperty("includeSynonyms", String.valueOf(datasourceConfiguration.includeSynonyms));
        props.setProperty("defaultNChar", String.valueOf(datasourceConfiguration.defaultNChar));
        props.setProperty("AccumulateBatchResult", String.valueOf(datasourceConfiguration.accumulateBatchResult));
        ds.setConnectionProperties(props);
        dataSources.add(ds);
        return ds;
    }

    @Override
    @PreDestroy
    public void close() {
        for (PoolDataSource dataSource : dataSources) {
            try {
                UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager().destroyConnectionPool(dataSource.getConnectionPoolName());
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Error closing data source [" + dataSource + "]: " + e.getMessage(), e);
                }
            }
        }
    }

}
