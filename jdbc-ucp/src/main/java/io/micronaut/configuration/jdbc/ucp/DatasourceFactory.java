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
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates an ucp data source for each configuration bean.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@Factory
public class DatasourceFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private List<PoolDataSource> dataSources = new ArrayList<>(2);
    private final ApplicationContext applicationContext;
    private final UniversalConnectionPoolManager connectionPoolManager;

    /**
     * Default constructor.
     *
     * @param applicationContext The application context
     */
    public DatasourceFactory(ApplicationContext applicationContext) throws UniversalConnectionPoolException {
        this.applicationContext = applicationContext;
        this.connectionPoolManager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
    }

    /**
     * Method to get a PoolDataSource from the {@link DatasourceConfiguration}.
     *
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @return A {@link PoolDataSource}
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    public PoolDataSource dataSource(DatasourceConfiguration datasourceConfiguration) throws UniversalConnectionPoolException {
        PoolDataSource ds = datasourceConfiguration.delegate;
        dataSources.add(ds);

        return ds;
    }

    @Override
    @PreDestroy
    public void close() {
        for (PoolDataSource dataSource : dataSources) {
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
