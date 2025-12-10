/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.configuration.jdbc.unpooled;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

/**
 * A simple unpooled {@link DataSource} implementation that creates a new database connection
 * for each {@link #getConnection()} call using the JDBC {@link DriverManager}.
 *
 * <p>This implementation does not pool connections. Each call to {@link #getConnection()}
 * creates a new physical database connection, and each call to {@link Connection#close()}
 * actually closes the connection instead of returning it to a pool.</p>
 *
 * <p><strong>Warning:</strong> This implementation has significant performance implications.
 * Creating a new connection typically takes 10-100ms, compared to ~1ms for retrieving a
 * connection from a pool. This should only be used for:</p>
 * <ul>
 *   <li>Testing scenarios that require clean connection state</li>
 *   <li>Serverless/FaaS applications with short-lived executions</li>
 *   <li>Very low-volume applications (&lt;1 request/second)</li>
 *   <li>Educational or prototype applications</li>
 * </ul>
 *
 * <p>For production applications with normal load, use a pooled datasource implementation
 * such as HikariCP, Tomcat JDBC Pool, or Apache DBCP.</p>
 *
 * @author Micronaut Team
 * @since 6.3.0
 */
public class UnpooledDataSource implements DataSource {

    private static final Logger LOG = LoggerFactory.getLogger(UnpooledDataSource.class);

    private final String url;
    private volatile String username;
    private volatile String password;
    private final String driverClassName;
    private final Properties dataSourceProperties;
    private int loginTimeout = 0;
    private PrintWriter logWriter;

    /**
     * Creates a new unpooled datasource.
     *
     * @param url The JDBC URL
     * @param username The database username (may be null)
     * @param password The database password (may be null)
     * @param driverClassName The JDBC driver class name
     * @param dataSourceProperties Additional datasource properties (may be null)
     */
    public UnpooledDataSource(
            @NonNull String url,
            @Nullable String username,
            @Nullable String password,
            @Nullable String driverClassName,
            @Nullable Properties dataSourceProperties) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
        this.dataSourceProperties = dataSourceProperties != null ? dataSourceProperties : new Properties();

        // Load the driver class if specified
        if (driverClassName != null) {
            try {
                Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                LOG.warn("JDBC driver class [{}] not found on classpath", driverClassName);
            }
        }
    }

    @Override
    @NonNull
    public Connection getConnection() throws SQLException {
        return getConnection(this.username, this.password);
    }

    @Override
    @NonNull
    public Connection getConnection(@Nullable String username, @Nullable String password) throws SQLException {
        Properties props = new Properties(dataSourceProperties);
        if (username != null) {
            props.setProperty("user", username);
        }
        if (password != null) {
            props.setProperty("password", password);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creating new database connection to [{}]", url);
        }

        // Get driver and create connection
        Driver driver = DriverManager.getDriver(url);
        Connection connection = driver.connect(url, props);

        if (connection == null) {
            throw new SQLException("Driver returned null connection for URL: " + url);
        }

        return connection;
    }

    /**
     * Updates the credentials used for new connections.
     * This method is called when datasource credentials are refreshed.
     *
     * @param username The new username (may be null to leave unchanged)
     * @param password The new password (may be null to leave unchanged)
     */
    public void updateCredentials(@Nullable String username, @Nullable String password) {
        if (username != null) {
            this.username = username;
        }
        if (password != null) {
            this.password = password;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updated credentials for datasource [{}]", url);
        }
    }

    /**
     * Gets the JDBC URL.
     *
     * @return The JDBC URL
     */
    @NonNull
    public String getUrl() {
        return url;
    }

    /**
     * Gets the username.
     *
     * @return The username (may be null)
     */
    @Nullable
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username The username
     */
    public void setUsername(@Nullable String username) {
        this.username = username;
    }

    /**
     * Gets the password.
     *
     * @return The password (may be null)
     */
    @Nullable
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password The password
     */
    public void setPassword(@Nullable String password) {
        this.password = password;
    }

    /**
     * Gets the driver class name.
     *
     * @return The driver class name (may be null)
     */
    @Nullable
    public String getDriverClassName() {
        return driverClassName;
    }

    @Override
    public PrintWriter getLogWriter() {
        return logWriter;
    }

    @Override
    public void setLogWriter(PrintWriter out) {
        this.logWriter = out;
    }

    @Override
    public void setLoginTimeout(int seconds) {
        this.loginTimeout = seconds;
    }

    @Override
    public int getLoginTimeout() {
        return loginTimeout;
    }

    @Override
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("UnpooledDataSource does not support getParentLogger");
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("UnpooledDataSource does not wrap " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }
}
