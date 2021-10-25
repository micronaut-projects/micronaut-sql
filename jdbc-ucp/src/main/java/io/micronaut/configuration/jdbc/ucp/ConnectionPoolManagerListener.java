/*
 * Copyright 2017-2021 original authors
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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.jdbc.PoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;


/**
 * {@link BeanCreatedEventListener} that starts the {@link PoolDataSource} using the {@link UniversalConnectionPoolManager}.
 *
 * @author Pavol Gressa
 * @since 4.0.3
 */
@Singleton
@Requires(classes = PoolDataSource.class)
@Requires(property = "ucp-manager.enabled", notEquals = StringUtils.FALSE, defaultValue = StringUtils.TRUE)
@Internal
public class ConnectionPoolManagerListener implements BeanCreatedEventListener<DataSource>, Ordered {
    private static final int POSITION = Ordered.LOWEST_PRECEDENCE;
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionPoolManagerListener.class);

    private final UniversalConnectionPoolManager connectionPoolManager;

    public ConnectionPoolManagerListener(UniversalConnectionPoolManager connectionPoolManager) {
        this.connectionPoolManager = connectionPoolManager;
    }

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        final DataSource dataSource = event.getBean();
        if (dataSource instanceof PoolDataSource) {
            final PoolDataSource poolDataSource = (PoolDataSource) dataSource;
            final String poolName = poolDataSource.getConnectionPoolName();
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Creating connection pool named: {}", poolName);
                }
                connectionPoolManager.createConnectionPool((UniversalConnectionPoolAdapter) dataSource);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Starting connection pool named: {}", poolName);
                }
                connectionPoolManager.startConnectionPool(poolName);
                if (LOG.isInfoEnabled()) {
                    LOG.info("Connection pool named: {} started", poolName);
                }

            } catch (UniversalConnectionPoolException e) {
                throw new ConfigurationException(String.format("Failed to start connection pool named: %s", poolName), e);
            }
        }
        return event.getBean();
    }

    @Override
    public int getOrder() {
        return POSITION;
    }
}
