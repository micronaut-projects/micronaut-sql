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
package io.micronaut.jdbc;

import io.micronaut.core.annotation.Internal;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Shared SQLite-specific JDBC wrapping support.
 *
 * @since 7.0.0
 */
@Internal
public final class JdbcSqliteSupport {

    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    private static final String SQLITE_URL_PREFIX = "jdbc:sqlite:";
    private static final String SQLITE_READ_ONLY_MESSAGE_AFTER_CONNECT = "Cannot change read-only flag after establishing a connection";
    private static final String SQLITE_READ_ONLY_MESSAGE_IN_OPEN_TRANSACTION = "Cannot change Read-Only status of this connection";

    private JdbcSqliteSupport() {
    }

    /**
     * Determines whether the configured datasource targets SQLite.
     *
     * @param driverClassName The configured driver class name
     * @param jdbcUrl The configured JDBC URL
     * @return {@code true} if the datasource targets SQLite
     */
    public static boolean isSqlite(String driverClassName, String jdbcUrl) {
        if (SQLITE_DRIVER.equals(driverClassName)) {
            return true;
        }
        return jdbcUrl != null && jdbcUrl.startsWith(SQLITE_URL_PREFIX);
    }

    /**
     * Wraps a datasource so SQLite connections can tolerate unsupported post-connect
     * {@link Connection#setReadOnly(boolean)} transitions while preserving the underlying datasource
     * for resolver-based unwrapping.
     *
     * @param dataSource The datasource
     * @param driverClassName The configured driver class name
     * @param jdbcUrl The configured JDBC URL
     * @return The wrapped datasource, or the original datasource for non-SQLite configurations
     */
    public static DataSource wrapDataSource(DataSource dataSource, String driverClassName, String jdbcUrl) {
        if (!isSqlite(driverClassName, jdbcUrl) || dataSource instanceof SQLiteAwareDataSource) {
            return dataSource;
        }
        return new SQLiteAwareDataSource(dataSource);
    }

    /**
     * Wraps a single connection when the datasource targets SQLite so unsupported
     * {@link Connection#setReadOnly(boolean)} transitions can be handled consistently.
     *
     * @param connection The connection
     * @param driverClassName The configured driver class name
     * @param jdbcUrl The configured JDBC URL
     * @return The wrapped connection, or the original connection for non-SQLite configurations
     * @throws SQLException If reading the initial connection state fails
     */
    public static Connection wrapConnection(Connection connection, String driverClassName, String jdbcUrl) throws SQLException {
        return isSqlite(driverClassName, jdbcUrl) ? wrapSqliteConnection(connection) : connection;
    }

    /**
     * Wraps a SQLite connection to preserve successful read-only delegation while tolerating
     * the known sqlite-jdbc failures for unsupported post-connect read-only transitions.
     *
     * @param connection The SQLite connection
     * @return The wrapped SQLite connection
     * @throws SQLException If reading the initial connection state fails
     */
    public static Connection wrapSqliteConnection(Connection connection) throws SQLException {
        return (Connection) Proxy.newProxyInstance(
                connection.getClass().getClassLoader(),
                new Class<?>[]{Connection.class},
                new SQLiteReadOnlyInvocationHandler(connection)
        );
    }

    static final class SQLiteAwareDataSource implements DataSource {

        private final DataSource targetDataSource;

        private SQLiteAwareDataSource(DataSource targetDataSource) {
            this.targetDataSource = targetDataSource;
        }

        DataSource getTargetDataSource() {
            return targetDataSource;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return wrapSqliteConnection(targetDataSource.getConnection());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return wrapSqliteConnection(targetDataSource.getConnection(username, password));
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return targetDataSource.getLogWriter();
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            targetDataSource.setLogWriter(out);
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            targetDataSource.setLoginTimeout(seconds);
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return targetDataSource.getLoginTimeout();
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return targetDataSource.getParentLogger();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            if (iface.isInstance(targetDataSource)) {
                return iface.cast(targetDataSource);
            }
            return targetDataSource.unwrap(iface);
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return iface.isInstance(this) || iface.isInstance(targetDataSource) || targetDataSource.isWrapperFor(iface);
        }

        @Override
        public String toString() {
            return targetDataSource.toString();
        }
    }

    private static final class SQLiteReadOnlyInvocationHandler implements InvocationHandler {

        private final Connection target;
        private boolean readOnly;

        private SQLiteReadOnlyInvocationHandler(Connection target) throws SQLException {
            this.target = target;
            this.readOnly = target.isReadOnly();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String methodName = method.getName();
            if ("setReadOnly".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Boolean readOnlyFlag) {
                try {
                    target.setReadOnly(readOnlyFlag);
                } catch (SQLException e) {
                    if (!isUnsupportedSqliteReadOnlyToggle(e)) {
                        throw e;
                    }
                }
                readOnly = readOnlyFlag;
                return null;
            }
            if ("isReadOnly".equals(methodName) && (args == null || args.length == 0)) {
                return readOnly;
            }
            if ("unwrap".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> unwrapType) {
                if (unwrapType.isInstance(proxy)) {
                    return proxy;
                }
                if (unwrapType.isInstance(target)) {
                    return target;
                }
                return target.unwrap(unwrapType);
            }
            if ("isWrapperFor".equals(methodName) && args != null && args.length == 1 && args[0] instanceof Class<?> unwrapType) {
                return unwrapType.isInstance(proxy) || unwrapType.isInstance(target) || target.isWrapperFor(unwrapType);
            }
            if ("equals".equals(methodName) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(methodName) && (args == null || args.length == 0)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(methodName) && (args == null || args.length == 0)) {
                return target.toString();
            }
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }

        private static boolean isUnsupportedSqliteReadOnlyToggle(SQLException e) {
            String message = e.getMessage();
            return message != null
                    && (message.contains(SQLITE_READ_ONLY_MESSAGE_AFTER_CONNECT)
                    || message.contains(SQLITE_READ_ONLY_MESSAGE_IN_OPEN_TRANSACTION));
        }
    }
}
