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
package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.core.annotation.Internal;
import oracle.jdbc.OracleShardingKey;
import oracle.ucp.jdbc.PoolXADataSourceImpl;
import oracle.ucp.jdbc.UCPConnectionBuilder;
import oracle.ucp.jdbc.UCPXAConnectionBuilder;

import javax.sql.XAConnection;
import javax.sql.XAConnectionBuilder;
import java.io.Serial;
import java.sql.Connection;
import java.sql.ConnectionBuilder;
import java.sql.SQLException;
import java.sql.ShardingKey;
import java.time.Duration;
import java.util.Properties;

/**
 * A UCP XA pool whose {@link #getConnection()} methods also work. UCP disables them unless the WebLogic transaction
 * manager is present ({@code UCP-58: The method is disabled}), so the {@code DataSource} bean of an XA pool could not
 * open connections for local transactions, Micronaut Data or the health indicator. Here they borrow an XA connection
 * and return its logical connection, and closing that connection returns the XA connection to the pool. The
 * {@link #createConnectionBuilder() connection builder}, which passes sharding keys and labels, works the same way.
 *
 * @since 7.3.0
 */
@Internal
final class LocalConnectionPoolXADataSource extends PoolXADataSourceImpl {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public Connection getConnection() throws SQLException {
        return localConnection(getXAConnection());
    }

    @Override
    public Connection getConnection(String user, String password) throws SQLException {
        return localConnection(getXAConnection(user, password));
    }

    @Override
    public Connection getConnection(Properties labels) throws SQLException {
        return localConnection(getXAConnection(labels));
    }

    @Override
    public Connection getConnection(String user, String password, Properties labels) throws SQLException {
        return localConnection(getXAConnection(user, password, labels));
    }

    @Override
    public UCPConnectionBuilder createConnectionBuilder() {
        return new LocalConnectionBuilder(createXAConnectionBuilder());
    }

    private static Connection localConnection(XAConnection xaConnection) throws SQLException {
        try {
            return new XALocalConnection(xaConnection, xaConnection.getConnection());
        } catch (SQLException | RuntimeException e) {
            try {
                xaConnection.close();
            } catch (SQLException | RuntimeException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
    }

    /**
     * A connection builder that builds an XA connection with the same settings and returns its local connection.
     */
    private static final class LocalConnectionBuilder implements UCPConnectionBuilder {

        private UCPXAConnectionBuilder builder;

        private LocalConnectionBuilder(UCPXAConnectionBuilder builder) {
            this.builder = builder;
        }

        @Override
        public UCPConnectionBuilder user(String user) {
            builder = builder.user(user);
            return this;
        }

        @Override
        public UCPConnectionBuilder password(String password) {
            builder = builder.password(password);
            return this;
        }

        @Override
        public UCPConnectionBuilder labels(Properties labels) {
            builder = builder.labels(labels);
            return this;
        }

        @Override
        public UCPConnectionBuilder serviceName(String serviceName) {
            builder = builder.serviceName(serviceName);
            return this;
        }

        @Override
        public UCPConnectionBuilder proxyProperties(int proxyType, Properties proxyProperties) {
            builder = builder.proxyProperties(proxyType, proxyProperties);
            return this;
        }

        @Override
        public UCPConnectionBuilder shardingKey(OracleShardingKey shardingKey) {
            builder = builder.shardingKey(shardingKey);
            return this;
        }

        @Override
        public ConnectionBuilder shardingKey(ShardingKey shardingKey) {
            builder = ucp(builder.shardingKey(shardingKey));
            return this;
        }

        @Override
        public UCPConnectionBuilder superShardingKey(OracleShardingKey superShardingKey) {
            builder = builder.superShardingKey(superShardingKey);
            return this;
        }

        @Override
        public ConnectionBuilder superShardingKey(ShardingKey superShardingKey) {
            builder = ucp(builder.superShardingKey(superShardingKey));
            return this;
        }

        @Override
        public UCPConnectionBuilder pdbRoles(Properties pdbRoles) {
            builder = builder.pdbRoles(pdbRoles);
            return this;
        }

        @Override
        public UCPConnectionBuilder connectionWaitTimeout(int connectionWaitTimeout) {
            builder = builder.connectionWaitTimeout(connectionWaitTimeout);
            return this;
        }

        @Override
        public UCPConnectionBuilder connectionWaitDuration(Duration connectionWaitDuration) {
            builder = builder.connectionWaitDuration(connectionWaitDuration);
            return this;
        }

        @Override
        public Connection build() throws SQLException {
            return localConnection(builder.build());
        }

        private UCPXAConnectionBuilder ucp(XAConnectionBuilder next) {
            return next instanceof UCPXAConnectionBuilder ucpBuilder ? ucpBuilder : builder;
        }
    }
}
