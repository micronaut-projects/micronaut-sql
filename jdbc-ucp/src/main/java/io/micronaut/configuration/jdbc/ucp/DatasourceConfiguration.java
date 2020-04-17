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

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;
import oracle.jdbc.OracleShardingKeyBuilder;
import oracle.ucp.ConnectionAffinityCallback;
import oracle.ucp.ConnectionLabelingCallback;
import oracle.ucp.jdbc.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

/**
 * Allows the configuration of UCP JDBC data sources. All properties on
 * {@link PoolDataSourceImpl} are available to be configured.
 *
 * If the url, driver class, username, or password are missing, sensible defaults
 * will be provided when possible. If no configuration beyond the datasource name
 * is provided, an in memory datasource will be configured based on the available
 * drivers on the classpath.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
public class DatasourceConfiguration implements BasicJdbcConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceConfiguration.class);
    private CalculatedSettings calculatedSettings;
    private String name;
    PoolDataSource delegate = PoolDataSourceFactory.getPoolDataSource();

    /**
     * Constructor.
     * @param name name that comes from properties
     */
    public DatasourceConfiguration(@Parameter String name) {
        super();
        this.name = name;
        this.calculatedSettings = new CalculatedSettings(this);
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name the name of the datasource
     */
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDriverClassName() {
        return calculatedSettings.getDriverClassName();
    }

    @Override
    public String getConfiguredDriverClassName() {
        return delegate.getConnectionFactoryClassName();
    }

    @Override
    public String getConfiguredUrl() {
        return delegate.getURL();
    }

    @Override
    public String getUrl() {
        return calculatedSettings.getUrl();
    }

    @Override
    public String getUsername() {
        return calculatedSettings.getUsername();
    }

    /**
     * @param username the username
     * @throws SQLException
     */
    public void setUsername(String username) throws SQLException {
        delegate.setUser(username);
    }

    @Override
    public String getConfiguredUsername() {
        return delegate.getUser();
    }

    @Override
    public String getPassword() {
        return calculatedSettings.getPassword();
    }

    @Override
    public String getConfiguredPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    @Override
    public String getConfiguredValidationQuery() {
        return delegate.getSQLForValidateConnection();
    }

    public int getInitialPoolSize() {
        return delegate.getInitialPoolSize();
    }

    public void setInitialPoolSize(int i) throws SQLException {
        delegate.setInitialPoolSize(i);
    }

    public int getMinPoolSize() {
        return delegate.getMinPoolSize();
    }

    public void setMinPoolSize(int i) throws SQLException {
        delegate.setMinPoolSize(i);
    }

    public int getMaxPoolSize() {
        return delegate.getMaxPoolSize();
    }

    public void setMaxPoolSize(int i) throws SQLException {
        delegate.setMaxPoolSize(i);
    }

    public int getInactiveConnectionTimeout() {
        return delegate.getInactiveConnectionTimeout();
    }

    public void setInactiveConnectionTimeout(int i) throws SQLException {
        delegate.setInactiveConnectionTimeout(i);
    }

    public int getAbandonedConnectionTimeout() {
        return delegate.getAbandonedConnectionTimeout();
    }

    public void setAbandonedConnectionTimeout(int i) throws SQLException {
        delegate.setAbandonedConnectionTimeout(i);
    }

    public int getConnectionWaitTimeout() {
        return delegate.getConnectionWaitTimeout();
    }

    public void setConnectionWaitTimeout(int i) throws SQLException {
        delegate.setConnectionWaitTimeout(i);
    }

    public int getTimeToLiveConnectionTimeout() {
        return delegate.getTimeToLiveConnectionTimeout();
    }

    public void setTimeToLiveConnectionTimeout(int i) throws SQLException {
        delegate.setTimeToLiveConnectionTimeout(i);
    }

    public void setTimeoutCheckInterval(int i) throws SQLException {
        delegate.setTimeoutCheckInterval(i);
    }

    public int getTimeoutCheckInterval() {
        return delegate.getTimeoutCheckInterval();
    }

    public void setFastConnectionFailoverEnabled(boolean b) throws SQLException {
        delegate.setFastConnectionFailoverEnabled(b);
    }

    public boolean getFastConnectionFailoverEnabled() {
        return delegate.getFastConnectionFailoverEnabled();
    }

    public String getConnectionFactoryClassName() {
        return delegate.getConnectionFactoryClassName();
    }

    public void setConnectionFactoryClassName(String s) throws SQLException {
        delegate.setConnectionFactoryClassName(s);
    }

    public void setMaxStatements(int i) throws SQLException {
        delegate.setMaxStatements(i);
    }

    public int getMaxStatements() {
        return delegate.getMaxStatements();
    }

    public void setMaxIdleTime(int i) throws SQLException {
        delegate.setMaxIdleTime(i);
    }

    public int getMaxIdleTime() {
        return delegate.getMaxIdleTime();
    }

    public void setPropertyCycle(int i) throws SQLException {
        delegate.setPropertyCycle(i);
    }

    public int getPropertyCycle() {
        return delegate.getPropertyCycle();
    }

    public void setConnectionPoolName(String s) throws SQLException {
        delegate.setConnectionPoolName(s);
    }

    public String getConnectionPoolName() {
        return delegate.getConnectionPoolName();
    }

    public void setURL(String s) throws SQLException {
        delegate.setURL(s);
    }

    public String getURL() {
        return delegate.getURL();
    }

    public void setUser(String s) throws SQLException {
        delegate.setUser(s);
    }

    public String getUser() {
        return delegate.getUser();
    }

    public void setPassword(String s) throws SQLException {
        delegate.setPassword(s);
    }

    public void setServerName(String s) throws SQLException {
        delegate.setServerName(s);
    }

    public String getServerName() {
        return delegate.getServerName();
    }

    public void setPortNumber(int i) throws SQLException {
        delegate.setPortNumber(i);
    }

    public int getPortNumber() {
        return delegate.getPortNumber();
    }

    public void setDatabaseName(String s) throws SQLException {
        delegate.setDatabaseName(s);
    }

    public String getDatabaseName() {
        return delegate.getDatabaseName();
    }

    public void setDataSourceName(String s) throws SQLException {
        delegate.setDataSourceName(s);
    }

    public String getDataSourceName() {
        return delegate.getDataSourceName();
    }

    public void setDescription(String s) throws SQLException {
        delegate.setDescription(s);
    }

    public String getDescription() {
        return delegate.getDescription();
    }

    public void setNetworkProtocol(String s) throws SQLException {
        delegate.setNetworkProtocol(s);
    }

    public String getNetworkProtocol() {
        return delegate.getNetworkProtocol();
    }

    public void setRoleName(String s) throws SQLException {
        delegate.setRoleName(s);
    }

    public String getRoleName() {
        return delegate.getRoleName();
    }

    public void setValidateConnectionOnBorrow(boolean b) throws SQLException {
        delegate.setValidateConnectionOnBorrow(b);
    }

    public boolean getValidateConnectionOnBorrow() {
        return delegate.getValidateConnectionOnBorrow();
    }

    public void setSQLForValidateConnection(String s) throws SQLException {
        delegate.setSQLForValidateConnection(s);
    }

    public String getSQLForValidateConnection() {
        return delegate.getSQLForValidateConnection();
    }

    public int getConnectionHarvestTriggerCount() {
        return delegate.getConnectionHarvestTriggerCount();
    }

    public void setConnectionHarvestTriggerCount(int i) throws SQLException {
        delegate.setConnectionHarvestTriggerCount(i);
    }

    public int getConnectionHarvestMaxCount() {
        return delegate.getConnectionHarvestMaxCount();
    }

    public void setConnectionHarvestMaxCount(int i) throws SQLException {
        delegate.setConnectionHarvestMaxCount(i);
    }

    public int getAvailableConnectionsCount() throws SQLException {
        return delegate.getAvailableConnectionsCount();
    }

    public int getBorrowedConnectionsCount() throws SQLException {
        return delegate.getBorrowedConnectionsCount();
    }

    public String getONSConfiguration() throws SQLException {
        return delegate.getONSConfiguration();
    }

    public void setONSConfiguration(String s) throws SQLException {
        delegate.setONSConfiguration(s);
    }

    public Connection getConnection(Properties properties) throws SQLException {
        return delegate.getConnection(properties);
    }

    public Connection getConnection(String s, String s1, Properties properties) throws SQLException {
        return delegate.getConnection(s, s1, properties);
    }

    public void registerConnectionLabelingCallback(ConnectionLabelingCallback connectionLabelingCallback) throws SQLException {
        delegate.registerConnectionLabelingCallback(connectionLabelingCallback);
    }

    public void removeConnectionLabelingCallback() throws SQLException {
        delegate.removeConnectionLabelingCallback();
    }

    public void registerConnectionAffinityCallback(ConnectionAffinityCallback connectionAffinityCallback) throws SQLException {
        delegate.registerConnectionAffinityCallback(connectionAffinityCallback);
    }

    public void removeConnectionAffinityCallback() throws SQLException {
        delegate.removeConnectionAffinityCallback();
    }

    public Properties getConnectionProperties() {
        return delegate.getConnectionProperties();
    }

    public String getConnectionProperty(String s) {
        return delegate.getConnectionProperty(s);
    }

    public void setConnectionProperty(String s, String s1) throws SQLException {
        delegate.setConnectionProperty(s, s1);
    }

    public void setConnectionProperties(Properties properties) throws SQLException {
        delegate.setConnectionProperties(properties);
    }

    public Properties getConnectionFactoryProperties() {
        return delegate.getConnectionFactoryProperties();
    }

    public String getConnectionFactoryProperty(String s) {
        return delegate.getConnectionFactoryProperty(s);
    }

    public void setConnectionFactoryProperty(String s, String s1) throws SQLException {
        delegate.setConnectionFactoryProperty(s, s1);
    }

    public void setConnectionFactoryProperties(Properties properties) throws SQLException {
        delegate.setConnectionFactoryProperties(properties);
    }

    public long getMaxConnectionReuseTime() {
        return delegate.getMaxConnectionReuseTime();
    }

    public void setMaxConnectionReuseTime(long l) throws SQLException {
        delegate.setMaxConnectionReuseTime(l);
    }

    public int getMaxConnectionReuseCount() {
        return delegate.getMaxConnectionReuseCount();
    }

    public void setMaxConnectionReuseCount(int i) throws SQLException {
        delegate.setMaxConnectionReuseCount(i);
    }

    public JDBCConnectionPoolStatistics getStatistics() {
        return delegate.getStatistics();
    }

    public void registerConnectionInitializationCallback(ConnectionInitializationCallback connectionInitializationCallback) throws SQLException {
        delegate.registerConnectionInitializationCallback(connectionInitializationCallback);
    }

    public void unregisterConnectionInitializationCallback() throws SQLException {
        delegate.unregisterConnectionInitializationCallback();
    }

    public ConnectionInitializationCallback getConnectionInitializationCallback() {
        return delegate.getConnectionInitializationCallback();
    }

    public int getConnectionLabelingHighCost() {
        return delegate.getConnectionLabelingHighCost();
    }

    public void setConnectionLabelingHighCost(int i) throws SQLException {
        delegate.setConnectionLabelingHighCost(i);
    }

    public int getHighCostConnectionReuseThreshold() {
        return delegate.getHighCostConnectionReuseThreshold();
    }

    public void setHighCostConnectionReuseThreshold(int i) throws SQLException {
        delegate.setHighCostConnectionReuseThreshold(i);
    }

    public UCPConnectionBuilder createConnectionBuilder() {
        return delegate.createConnectionBuilder();
    }

    public OracleShardingKeyBuilder createShardingKeyBuilder() {
        return delegate.createShardingKeyBuilder();
    }

    public int getConnectionRepurposeThreshold() {
        return delegate.getConnectionRepurposeThreshold();
    }

    public void setConnectionRepurposeThreshold(int i) throws SQLException {
        delegate.setConnectionRepurposeThreshold(i);
    }

    public Properties getPdbRoles() {
        return delegate.getPdbRoles();
    }

    public String getServiceName() {
        return delegate.getServiceName();
    }

    public int getSecondsToTrustIdleConnection() {
        return delegate.getSecondsToTrustIdleConnection();
    }

    public void setSecondsToTrustIdleConnection(int i) throws SQLException {
        delegate.setSecondsToTrustIdleConnection(i);
    }

    public void reconfigureDataSource(Properties properties) throws SQLException {
        delegate.reconfigureDataSource(properties);
    }

    public int getMaxConnectionsPerService() {
        return delegate.getMaxConnectionsPerService();
    }

    public int getQueryTimeout() {
        return delegate.getQueryTimeout();
    }

    public void setQueryTimeout(int i) throws SQLException {
        delegate.setQueryTimeout(i);
    }

    public int getMaxConnectionsPerShard() {
        return delegate.getMaxConnectionsPerShard();
    }

    public void setMaxConnectionsPerShard(int i) throws SQLException {
        delegate.setMaxConnectionsPerShard(i);
    }

    public void setShardingMode(boolean b) throws SQLException {
        delegate.setShardingMode(b);
    }

    public boolean getShardingMode() {
        return delegate.getShardingMode();
    }

    public void setConnectionValidationTimeout(int i) throws SQLException {
        delegate.setConnectionValidationTimeout(i);
    }

    public int getConnectionValidationTimeout() {
        return delegate.getConnectionValidationTimeout();
    }

    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
