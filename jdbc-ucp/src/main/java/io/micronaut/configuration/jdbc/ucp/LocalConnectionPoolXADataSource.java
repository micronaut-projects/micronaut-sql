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
import oracle.ucp.jdbc.PoolXADataSourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.ConnectionEvent;
import javax.sql.ConnectionEventListener;
import javax.sql.XAConnection;
import java.io.Serial;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * A UCP XA pool whose {@link #getConnection()} methods also work. UCP disables them unless the WebLogic transaction
 * manager is present ({@code UCP-58: The method is disabled}), so the {@code DataSource} bean of an XA pool could not
 * open connections for local transactions, Micronaut Data or the health indicator. Here they borrow an XA connection
 * and return its logical connection, and closing that connection returns the XA connection to the pool.
 *
 * @since 7.3.0
 */
@Internal
final class LocalConnectionPoolXADataSource extends PoolXADataSourceImpl {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(LocalConnectionPoolXADataSource.class);

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

    private static Connection localConnection(XAConnection xaConnection) throws SQLException {
        Connection connection;
        try {
            connection = xaConnection.getConnection();
        } catch (SQLException | RuntimeException e) {
            close(xaConnection, e);
            throw e;
        }
        // closing only the logical connection would leave the XA connection borrowed
        xaConnection.addConnectionEventListener(new ConnectionEventListener() {
            @Override
            public void connectionClosed(ConnectionEvent event) {
                close(xaConnection, null);
            }

            @Override
            public void connectionErrorOccurred(ConnectionEvent event) {
                // the pool handles connections that failed
            }
        });
        return connection;
    }

    private static void close(XAConnection xaConnection, Exception failure) {
        try {
            xaConnection.close();
        } catch (SQLException | RuntimeException e) {
            if (failure != null) {
                failure.addSuppressed(e);
            } else if (LOG.isWarnEnabled()) {
                LOG.warn("Unable to return an XA connection to its pool: {}", e.getMessage(), e);
            }
        }
    }
}
