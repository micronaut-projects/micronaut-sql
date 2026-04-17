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
package io.micronaut.configuration.jdbc.unpooled;

import io.micronaut.core.util.StringUtils;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * A simple unpooled {@link DataSource} backed by {@link DriverManager}.
 *
 * @author Micronaut
 * @since 7.0.0
 */
public class DriverManagerDataSource implements DataSource {

    private static final Logger LOGGER = Logger.getLogger(DriverManagerDataSource.class.getName());
    private static final AtomicReference<Integer> CONFIGURED_LOGIN_TIMEOUT = new AtomicReference<>();

    private final String name;
    private final Properties dataSourceProperties = new Properties();

    private volatile String url;
    private volatile String driverClassName;
    private volatile String username;
    private volatile String password;

    /**
     * @param configuration The datasource configuration
     */
    public DriverManagerDataSource(DatasourceConfiguration configuration) {
        this.name = configuration.getName();
        this.url = configuration.getUrl();
        this.driverClassName = configuration.getDriverClassName();
        this.username = configuration.getUsername();
        this.password = configuration.getPassword();
        configuration.getDataSourceProperties().forEach(dataSourceProperties::setProperty);
        if (configuration.getLoginTimeout() != null) {
            setLoginTimeout(configuration.getLoginTimeout());
        }
        initializeDriver(configuration.getDriverClass(), driverClassName);
    }

    /**
     * @return The datasource name
     */
    public String getName() {
        return name;
    }

    /**
     * @return The JDBC URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url The JDBC URL
     */
    public void setUrl(String url) {
        this.url = Objects.requireNonNull(url, "url");
    }

    /**
     * @return The configured driver class name
     */
    public String getDriverClassName() {
        return driverClassName;
    }

    /**
     * @param driverClassName The configured driver class name
     */
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = Objects.requireNonNull(driverClassName, "driverClassName");
        initializeDriver(null, driverClassName);
    }

    /**
     * @return The configured username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username The configured username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return The configured password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password The configured password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return Additional datasource properties passed to the JDBC driver
     */
    public Properties getDataSourceProperties() {
        Properties copy = new Properties();
        copy.putAll(dataSourceProperties);
        return copy;
    }

    /**
     * Replace all configured datasource properties.
     *
     * @param properties The new datasource properties
     */
    public void setDataSourceProperties(Map<String, String> properties) {
        dataSourceProperties.clear();
        if (properties != null) {
            properties.forEach((key, value) -> {
                if (value != null) {
                    dataSourceProperties.setProperty(key, value);
                }
            });
        }
    }

    /**
     * Add a single datasource property.
     *
     * @param key The property name
     * @param value The property value
     */
    public void addDataSourceProperty(String key, String value) {
        dataSourceProperties.setProperty(key, value);
    }

    /**
     * @param key The property name
     * @return Whether the configured datasource properties contain the given key
     */
    public boolean hasDataSourceProperty(String key) {
        return dataSourceProperties.containsKey(key);
    }

    @Override
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, buildConnectionProperties(username, password));
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return DriverManager.getConnection(url, buildConnectionProperties(username, password));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return DriverManager.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        DriverManager.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) {
        Integer configuredTimeout = CONFIGURED_LOGIN_TIMEOUT.get();
        if (configuredTimeout == null) {
            if (CONFIGURED_LOGIN_TIMEOUT.compareAndSet(null, seconds)) {
                DriverManager.setLoginTimeout(seconds);
            } else {
                setLoginTimeout(seconds);
            }
            return;
        }
        if (configuredTimeout == seconds) {
            LOGGER.info("DriverManager login timeout already configured to " + seconds + " seconds; repeated call from datasource '" + name + "' ignored.");
            return;
        }
        LOGGER.warning("Ignoring loginTimeout " + seconds + " for datasource '" + name
            + "' because DriverManager login timeout is a JVM-global setting and is already set to " + configuredTimeout + ".");
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return DriverManager.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        try {
            return DriverManager.getDriver(url).getParentLogger();
        } catch (SQLException e) {
            SQLFeatureNotSupportedException exception =
                new SQLFeatureNotSupportedException("Unable to resolve parent logger for JDBC driver");
            exception.initCause(e);
            throw exception;
        }
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("Datasource cannot be unwrapped to " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    private Properties buildConnectionProperties(String connectionUsername, String connectionPassword) {
        Properties properties = new Properties();
        properties.putAll(dataSourceProperties);
        if (StringUtils.isNotEmpty(connectionUsername)) {
            properties.setProperty("user", connectionUsername);
        }
        if (connectionPassword != null) {
            properties.setProperty("password", connectionPassword);
        }
        return properties;
    }

    private void initializeDriver(Class<? extends Driver> driverClass, String driverClassName) {
        if (driverClass != null) {
            this.driverClassName = driverClass.getName();
            return;
        }
        try {
            Class.forName(driverClassName, true, getClass().getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new io.micronaut.context.exceptions.ConfigurationException("Error configuring data source '" + name + "'. The driver class '" + driverClassName + "' was not found on the classpath", e);
        }
    }
}
