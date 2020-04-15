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
        ds.setConnectionFactoryClassName(datasourceConfiguration.getConnectionFactoryClassName());
        ds.setURL(datasourceConfiguration.getUrl());
        ds.setUser(datasourceConfiguration.getUsername());
        ds.setPassword(datasourceConfiguration.getPassword());
        ds.setConnectionPoolName(datasourceConfiguration.getConnectionPoolName());
        ds.setInitialPoolSize(datasourceConfiguration.getInitialPoolSize());
        ds.setMinPoolSize(datasourceConfiguration.getMinPoolSize());
        ds.setMaxPoolSize(datasourceConfiguration.getMaxPoolSize());
        ds.setTimeoutCheckInterval(datasourceConfiguration.getTimeoutCheckInterval());
        ds.setInactiveConnectionTimeout(datasourceConfiguration.getInactiveConnectionTimeout());
        Properties props = new Properties();
        props.setProperty("fixedString", String.valueOf(datasourceConfiguration.isFixedString()));
        props.setProperty("remarksReporting", String.valueOf(datasourceConfiguration.isRemarksReporting()));
        props.setProperty("restrictGetTables", String.valueOf(datasourceConfiguration.isRestrictGetTables()));
        props.setProperty("includeSynonyms", String.valueOf(datasourceConfiguration.isIncludeSynonyms()));
        props.setProperty("defaultNChar", String.valueOf(datasourceConfiguration.isDefaultNChar()));
        props.setProperty("AccumulateBatchResult", String.valueOf(datasourceConfiguration.isAccumulateBatchResult()));
        ds.setConnectionProperties(props);

        if (datasourceConfiguration.getConnectionWaitTimeout() != null) ds.setConnectionWaitTimeout(datasourceConfiguration.getConnectionWaitTimeout());
        if (datasourceConfiguration.getLoginTimeout() != null) ds.setLoginTimeout(datasourceConfiguration.getLoginTimeout());
        if (datasourceConfiguration.getMaxConnectionReuseCount() != null) ds.setMaxConnectionReuseCount(datasourceConfiguration.getMaxConnectionReuseCount());
        if (datasourceConfiguration.getMaxConnectionReuseTime() != null) ds.setMaxConnectionReuseTime(datasourceConfiguration.getMaxConnectionReuseTime());
        if (datasourceConfiguration.getMaxIdleTime() != null) ds.setMaxIdleTime(datasourceConfiguration.getMaxIdleTime());
        if (datasourceConfiguration.getMaxStatements() != null) ds.setMaxStatements(datasourceConfiguration.getMaxStatements());
        if (datasourceConfiguration.getNetworkProtocol() != null) ds.setNetworkProtocol(datasourceConfiguration.getNetworkProtocol());
        if (datasourceConfiguration.getOnsConfiguration() != null) ds.setONSConfiguration(datasourceConfiguration.getOnsConfiguration());
        if (datasourceConfiguration.getPortNumber() != null) ds.setPortNumber(datasourceConfiguration.getPortNumber());
        if (datasourceConfiguration.getPropertyCycle() != null) ds.setPropertyCycle(datasourceConfiguration.getPropertyCycle());
        if (datasourceConfiguration.getRoleName() != null) ds.setRoleName(datasourceConfiguration.getRoleName());
        if (datasourceConfiguration.getServerName() != null) ds.setServerName(datasourceConfiguration.getServerName());
        if (datasourceConfiguration.getSqlForValidateConnection() != null) ds.setSQLForValidateConnection(datasourceConfiguration.getSqlForValidateConnection());
        if (datasourceConfiguration.isFastConnectionFailoverEnabled() != null) ds.setFastConnectionFailoverEnabled(datasourceConfiguration.isFastConnectionFailoverEnabled());
        if (datasourceConfiguration.isValidateConnectionOnBorrow() != null) ds.setValidateConnectionOnBorrow(datasourceConfiguration.isValidateConnectionOnBorrow());

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
