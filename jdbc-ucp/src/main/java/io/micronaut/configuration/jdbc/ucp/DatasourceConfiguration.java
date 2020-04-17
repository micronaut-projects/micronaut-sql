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
    PoolDataSource delegate = PoolDataSourceFactory.getPoolDataSource();
    private CalculatedSettings calculatedSettings;
    private String name;

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
     *
     * @param name the name of the datasource
     * @throws SQLException an sql exception
     */
    public void setName(String name) throws SQLException {
        this.name = name;
        delegate.setConnectionPoolName(name);
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
     * @throws SQLException an sql exception
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

    /**
     *
     * @return the initial pool size
     */
    public int getInitialPoolSize() {
        return delegate.getInitialPoolSize();
    }

    /**
     *
     * @param i the initial pool size
     * @throws SQLException an sql exception
     */
    public void setInitialPoolSize(int i) throws SQLException {
        delegate.setInitialPoolSize(i);
    }

    /**
     *
     * @return min pool size
     */
    public int getMinPoolSize() {
        return delegate.getMinPoolSize();
    }

    /**
     *
     * @param i the min pool size
     * @throws SQLException an sql exception
     */
    public void setMinPoolSize(int i) throws SQLException {
        delegate.setMinPoolSize(i);
    }

    /**
     *
     * @return max pool size
     */
    public int getMaxPoolSize() {
        return delegate.getMaxPoolSize();
    }

    /**
     *
     * @param i max pool size
     * @throws SQLException an sql exception
     */
    public void setMaxPoolSize(int i) throws SQLException {
        delegate.setMaxPoolSize(i);
    }

    /**
     *
     * @return inactive connection timeout
     */
    public int getInactiveConnectionTimeout() {
        return delegate.getInactiveConnectionTimeout();
    }

    /**
     *
     * @param i inactive connection timeout
     * @throws SQLException an sql exception
     */
    public void setInactiveConnectionTimeout(int i) throws SQLException {
        delegate.setInactiveConnectionTimeout(i);
    }

    /**
     *
     * @return abandoned Connection Timeout
     */
    public int getAbandonedConnectionTimeout() {
        return delegate.getAbandonedConnectionTimeout();
    }

    /**
     *
     * @param i abandoned Connection Timeout
     * @throws SQLException an sql exception
     */
    public void setAbandonedConnectionTimeout(int i) throws SQLException {
        delegate.setAbandonedConnectionTimeout(i);
    }

    /**
     *
     * @return ConnectionWaitTimeout
     */
    public int getConnectionWaitTimeout() {
        return delegate.getConnectionWaitTimeout();
    }

    /**
     *
     * @param i ConnectionWaitTimeout
     * @throws SQLException an sql exception
     */
    public void setConnectionWaitTimeout(int i) throws SQLException {
        delegate.setConnectionWaitTimeout(i);
    }

    /**
     *
     * @return TimeToLiveConnectionTimeout
     */
    public int getTimeToLiveConnectionTimeout() {
        return delegate.getTimeToLiveConnectionTimeout();
    }

    /**
     *
     * @param i TimeToLiveConnectionTimeout
     * @throws SQLException an sql exception
     */
    public void setTimeToLiveConnectionTimeout(int i) throws SQLException {
        delegate.setTimeToLiveConnectionTimeout(i);
    }

    /**
     *
     * @param i TimeoutCheckInterval
     * @throws SQLException an sql exception
     */
    public void setTimeoutCheckInterval(int i) throws SQLException {
        delegate.setTimeoutCheckInterval(i);
    }

    /**
     *
     * @return TimeoutCheckInterval
     */
    public int getTimeoutCheckInterval() {
        return delegate.getTimeoutCheckInterval();
    }

    /**
     *
     * @param b FastConnectionFailoverEnabled
     * @throws SQLException an sql exception
     */
    public void setFastConnectionFailoverEnabled(boolean b) throws SQLException {
        delegate.setFastConnectionFailoverEnabled(b);
    }

    /**
     *
     * @return FastConnectionFailoverEnabled
     */
    public boolean getFastConnectionFailoverEnabled() {
        return delegate.getFastConnectionFailoverEnabled();
    }

    /**
     *
     * @return ConnectionFactoryClassName
     */
    public String getConnectionFactoryClassName() {
        return delegate.getConnectionFactoryClassName();
    }

    /**
     *
     * @param s ConnectionFactoryClassName
     * @throws SQLException an sql exception
     */
    public void setConnectionFactoryClassName(String s) throws SQLException {
        delegate.setConnectionFactoryClassName(s);
    }

    /**
     *
     * @param i MaxStatements
     * @throws SQLException an sql exception
     */
    public void setMaxStatements(int i) throws SQLException {
        delegate.setMaxStatements(i);
    }

    /**
     *
     * @return MaxStatements
     */
    public int getMaxStatements() {
        return delegate.getMaxStatements();
    }

    /**
     *
     * @param i MaxStatements
     * @throws SQLException an sql exception
     */
    public void setMaxIdleTime(int i) throws SQLException {
        delegate.setMaxIdleTime(i);
    }

    /**
     *
     * @return MaxIdleTime
     */
    public int getMaxIdleTime() {
        return delegate.getMaxIdleTime();
    }

    /**
     *
     * @param i PropertyCycle
     * @throws SQLException an sql exception
     */
    public void setPropertyCycle(int i) throws SQLException {
        delegate.setPropertyCycle(i);
    }

    /**
     *
     * @return PropertyCycle
     */
    public int getPropertyCycle() {
        return delegate.getPropertyCycle();
    }

    /**
     *
     * @param s ConnectionPoolName
     * @throws SQLException an sql exception
     */
    public void setConnectionPoolName(String s) throws SQLException {
        delegate.setConnectionPoolName(s);
    }

    /**
     *
     * @return ConnectionPoolName
     */
    public String getConnectionPoolName() {
        return delegate.getConnectionPoolName();
    }

    /**
     *
     * @param s the url
     * @throws SQLException an sql exception
     */
    public void setURL(String s) throws SQLException {
        delegate.setURL(s);
    }

    /**
     *
     * @param s the url
     * @throws SQLException an sql exception
     */
    public void setUrl(String s) throws SQLException {
        setURL(s);
    }

    /**
     *
     * @return the url
     */
    public String getURL() {
        return delegate.getURL();
    }

    /**
     *
     * @param s the user
     * @throws SQLException an sql exception
     */
    public void setUser(String s) throws SQLException {
        delegate.setUser(s);
    }

    /**
     *
     * @return the user
     */
    public String getUser() {
        return delegate.getUser();
    }

    /**
     *
     * @param s the password
     * @throws SQLException an sql exception
     */
    public void setPassword(String s) throws SQLException {
        delegate.setPassword(s);
    }

    /**
     *
     * @param s the server name
     * @throws SQLException an sql exception
     */
    public void setServerName(String s) throws SQLException {
        delegate.setServerName(s);
    }

    /**
     *
     * @return the server name
     */
    public String getServerName() {
        return delegate.getServerName();
    }

    /**
     *
     * @param i the port number
     * @throws SQLException an sql exception
     */
    public void setPortNumber(int i) throws SQLException {
        delegate.setPortNumber(i);
    }

    /**
     *
     * @return the port number
     */
    public int getPortNumber() {
        return delegate.getPortNumber();
    }

    /**
     *
     * @param s the db name
     * @throws SQLException an sql exception
     */
    public void setDatabaseName(String s) throws SQLException {
        delegate.setDatabaseName(s);
    }

    /**
     *
     * @return the db name
     */
    public String getDatabaseName() {
        return delegate.getDatabaseName();
    }

    /**
     *
     * @param s the datasource name
     * @throws SQLException an sql exception
     */
    public void setDataSourceName(String s) throws SQLException {
        delegate.setDataSourceName(s);
    }

    /**
     *
      * @return the ds name
     */
    public String getDataSourceName() {
        return delegate.getDataSourceName();
    }

    /**
     *
     * @param s description
     * @throws SQLException an sql exception
     */
    public void setDescription(String s) throws SQLException {
        delegate.setDescription(s);
    }

    /**
     *
     * @return description
     */
    public String getDescription() {
        return delegate.getDescription();
    }

    /**
     *
     * @param s the network protocol
     * @throws SQLException an sql exception
     */
    public void setNetworkProtocol(String s) throws SQLException {
        delegate.setNetworkProtocol(s);
    }

    /**
     *
     * @return the network protocol
     */
    public String getNetworkProtocol() {
        return delegate.getNetworkProtocol();
    }

    /**
     *
     * @param s the role name
     * @throws SQLException an sql exception
     */
    public void setRoleName(String s) throws SQLException {
        delegate.setRoleName(s);
    }

    /**
     *
     * @return the role name
     */
    public String getRoleName() {
        return delegate.getRoleName();
    }

    /**
     *
     * @param b ValidateConnectionOnBorrow
     * @throws SQLException an sql exception
     */
    public void setValidateConnectionOnBorrow(boolean b) throws SQLException {
        delegate.setValidateConnectionOnBorrow(b);
    }

    /**
     *
     * @return ValidateConnectionOnBorrow
     */
    public boolean getValidateConnectionOnBorrow() {
        return delegate.getValidateConnectionOnBorrow();
    }

    /**
     *
     * @param s SQLForValidateConnection
     * @throws SQLException an sql exception
     */
    public void setSQLForValidateConnection(String s) throws SQLException {
        delegate.setSQLForValidateConnection(s);
    }

    /**
     *
     * @return SQLForValidateConnection
     */
    public String getSQLForValidateConnection() {
        return delegate.getSQLForValidateConnection();
    }

    /**
     *
     * @return ConnectionHarvestTriggerCount
     */
    public int getConnectionHarvestTriggerCount() {
        return delegate.getConnectionHarvestTriggerCount();
    }

    /**
     *
     * @param i ConnectionHarvestTriggerCount
     * @throws SQLException an sql exception
     */
    public void setConnectionHarvestTriggerCount(int i) throws SQLException {
        delegate.setConnectionHarvestTriggerCount(i);
    }

    /**
     *
     * @return ConnectionHarvestMaxCount
     */
    public int getConnectionHarvestMaxCount() {
        return delegate.getConnectionHarvestMaxCount();
    }

    /**
     *
     * @param i ConnectionHarvestMaxCount
     * @throws SQLException an sql exception
     */
    public void setConnectionHarvestMaxCount(int i) throws SQLException {
        delegate.setConnectionHarvestMaxCount(i);
    }

    /**
     *
     * @return AvailableConnectionsCount
     * @throws SQLException an sql exception
     */
    public int getAvailableConnectionsCount() throws SQLException {
        return delegate.getAvailableConnectionsCount();
    }

    /**
     *
     * @return BorrowedConnectionsCount
     * @throws SQLException an sql exception
     */
    public int getBorrowedConnectionsCount() throws SQLException {
        return delegate.getBorrowedConnectionsCount();
    }

    /**
     *
     * @return ONSConfiguration
     * @throws SQLException an sql exception
     */
    public String getONSConfiguration() throws SQLException {
        return delegate.getONSConfiguration();
    }

    /**
     *
     * @param s ONSConfiguration
     * @throws SQLException an sql exception
     */
    public void setONSConfiguration(String s) throws SQLException {
        delegate.setONSConfiguration(s);
    }

    /**
     *
     * @param properties the connection properties
     * @return a Connection
     * @throws SQLException an sql exception
     */
    public Connection getConnection(Properties properties) throws SQLException {
        return delegate.getConnection(properties);
    }

    /**
     *
     * @param s connect string
     * @param s1 connect string 2
     * @param properties props
     * @return a Connection
     * @throws SQLException an sql exception
     */
    public Connection getConnection(String s, String s1, Properties properties) throws SQLException {
        return delegate.getConnection(s, s1, properties);
    }

    /**
     *
     * @param connectionLabelingCallback connectionLabelingCallback
     * @throws SQLException an sql exception
     */
    public void registerConnectionLabelingCallback(ConnectionLabelingCallback connectionLabelingCallback) throws SQLException {
        delegate.registerConnectionLabelingCallback(connectionLabelingCallback);
    }

    /**
     * registerConnectionLabelingCallback.
     * @throws SQLException an sql exception
     */
    public void removeConnectionLabelingCallback() throws SQLException {
        delegate.removeConnectionLabelingCallback();
    }

    /**
     *
     * @param connectionAffinityCallback connectionAffinityCallback
     * @throws SQLException an sql exception
     */
    public void registerConnectionAffinityCallback(ConnectionAffinityCallback connectionAffinityCallback) throws SQLException {
        delegate.registerConnectionAffinityCallback(connectionAffinityCallback);
    }

    /**
     * removeConnectionAffinityCallback.
     * @throws SQLException an sql exception
     */
    public void removeConnectionAffinityCallback() throws SQLException {
        delegate.removeConnectionAffinityCallback();
    }

    /**
     *
     * @return Connection Properties
     */
    public Properties getConnectionProperties() {
        return delegate.getConnectionProperties();
    }

    /**
     *
     * @param s the property
     * @return the property
     */
    public String getConnectionProperty(String s) {
        return delegate.getConnectionProperty(s);
    }

    /**
     *
     * @param s prop key
     * @param s1 prop value
     * @throws SQLException an sql exception
     */
    public void setConnectionProperty(String s, String s1) throws SQLException {
        delegate.setConnectionProperty(s, s1);
    }

    /**
     *
     * @param properties the properties
     * @throws SQLException an sql exception
     */
    public void setConnectionProperties(Properties properties) throws SQLException {
        delegate.setConnectionProperties(properties);
    }

    /**
     *
     * @return the connection factory properties
     */
    public Properties getConnectionFactoryProperties() {
        return delegate.getConnectionFactoryProperties();
    }

    /**
     *
     * @param s key
     * @return the property value
     */
    public String getConnectionFactoryProperty(String s) {
        return delegate.getConnectionFactoryProperty(s);
    }

    /**
     *
     * @param s prop key
     * @param s1 prop value
     * @throws SQLException an sql exception
     */
    public void setConnectionFactoryProperty(String s, String s1) throws SQLException {
        delegate.setConnectionFactoryProperty(s, s1);
    }

    /**
     *
     * @param properties the props
     * @throws SQLException an sql exception
     */
    public void setConnectionFactoryProperties(Properties properties) throws SQLException {
        delegate.setConnectionFactoryProperties(properties);
    }

    /**
     *
     * @return max conn reuse time
     */
    public long getMaxConnectionReuseTime() {
        return delegate.getMaxConnectionReuseTime();
    }

    /**
     *
     * @param l max conn reuse time
     * @throws SQLException an sql exception
     */
    public void setMaxConnectionReuseTime(long l) throws SQLException {
        delegate.setMaxConnectionReuseTime(l);
    }

    /**
     *
     * @return MaxConnectionReuseCount
     */
    public int getMaxConnectionReuseCount() {
        return delegate.getMaxConnectionReuseCount();
    }

    /**
     *
     * @param i MaxConnectionReuseCount
     * @throws SQLException an sql exception
     */
    public void setMaxConnectionReuseCount(int i) throws SQLException {
        delegate.setMaxConnectionReuseCount(i);
    }

    /**
     *
     * @return stats
     */
    public JDBCConnectionPoolStatistics getStatistics() {
        return delegate.getStatistics();
    }

    /**
     *
      * @param connectionInitializationCallback connectionInitializationCallback
     * @throws SQLException an sql exception
     */
    public void registerConnectionInitializationCallback(ConnectionInitializationCallback connectionInitializationCallback) throws SQLException {
        delegate.registerConnectionInitializationCallback(connectionInitializationCallback);
    }

    /**
     * unregisterConnectionInitializationCallback.
     * @throws SQLException an sql exception
     */
    public void unregisterConnectionInitializationCallback() throws SQLException {
        delegate.unregisterConnectionInitializationCallback();
    }

    /**
     *
     * @return ConnectionInitializationCallback
     */
    public ConnectionInitializationCallback getConnectionInitializationCallback() {
        return delegate.getConnectionInitializationCallback();
    }

    /**
     *
     * @return getConnectionLabelingHighCost
     */
    public int getConnectionLabelingHighCost() {
        return delegate.getConnectionLabelingHighCost();
    }

    /**
     *
     * @param i ConnectionLabelingHighCost
     * @throws SQLException an sql exception
     */
    public void setConnectionLabelingHighCost(int i) throws SQLException {
        delegate.setConnectionLabelingHighCost(i);
    }

    /**
     *
     * @return HighCostConnectionReuseThreshold
     */
    public int getHighCostConnectionReuseThreshold() {
        return delegate.getHighCostConnectionReuseThreshold();
    }

    /**
     *
     * @param i HighCostConnectionReuseThreshold
     * @throws SQLException an sql exception
     */
    public void setHighCostConnectionReuseThreshold(int i) throws SQLException {
        delegate.setHighCostConnectionReuseThreshold(i);
    }

    /**
     *
     * @return UCPConnectionBuilder
     */
    public UCPConnectionBuilder createConnectionBuilder() {
        return delegate.createConnectionBuilder();
    }

    /**
     *
     * @return OracleShardingKeyBuilder
     */
    public OracleShardingKeyBuilder createShardingKeyBuilder() {
        return delegate.createShardingKeyBuilder();
    }

    /**
     *
     * @return ConnectionRepurposeThreshold
     */
    public int getConnectionRepurposeThreshold() {
        return delegate.getConnectionRepurposeThreshold();
    }

    /**
     *
     * @param i ConnectionRepurposeThreshold
     * @throws SQLException an sql exception
     */
    public void setConnectionRepurposeThreshold(int i) throws SQLException {
        delegate.setConnectionRepurposeThreshold(i);
    }

    /**
     *
     * @return PdbRoles
     */
    public Properties getPdbRoles() {
        return delegate.getPdbRoles();
    }

    /**
     *
     * @return ServiceName
     */
    public String getServiceName() {
        return delegate.getServiceName();
    }

    /**
     *
     * @return SecondsToTrustIdleConnection
     */
    public int getSecondsToTrustIdleConnection() {
        return delegate.getSecondsToTrustIdleConnection();
    }

    /**
     *
     * @param i SecondsToTrustIdleConnection
     * @throws SQLException an sql exception
     */
    public void setSecondsToTrustIdleConnection(int i) throws SQLException {
        delegate.setSecondsToTrustIdleConnection(i);
    }

    /**
     *
     * @param properties the props
     * @throws SQLException an sql exception
     */
    public void reconfigureDataSource(Properties properties) throws SQLException {
        delegate.reconfigureDataSource(properties);
    }

    /**
     *
     * @return MaxConnectionsPerService
     */
    public int getMaxConnectionsPerService() {
        return delegate.getMaxConnectionsPerService();
    }

    /**
     *
     * @return QueryTimeout
     */
    public int getQueryTimeout() {
        return delegate.getQueryTimeout();
    }

    /**
     *
     * @param i QueryTimeout
     * @throws SQLException an sql exception
     */
    public void setQueryTimeout(int i) throws SQLException {
        delegate.setQueryTimeout(i);
    }

    /**
     *
     * @return max conns per shard
     */
    public int getMaxConnectionsPerShard() {
        return delegate.getMaxConnectionsPerShard();
    }

    /**
     *
     * @param i max conns per shard
     * @throws SQLException an sql exception
     */
    public void setMaxConnectionsPerShard(int i) throws SQLException {
        delegate.setMaxConnectionsPerShard(i);
    }

    /**
     *
     * @param b ShardingMode
     * @throws SQLException an sql exception
     */
    public void setShardingMode(boolean b) throws SQLException {
        delegate.setShardingMode(b);
    }

    /**
     *
     * @return ShardingMode
     */
    public boolean getShardingMode() {
        return delegate.getShardingMode();
    }

    /**
     *
     * @param i ConnectionValidationTimeout
     * @throws SQLException an sql exception
     */
    public void setConnectionValidationTimeout(int i) throws SQLException {
        delegate.setConnectionValidationTimeout(i);
    }

    /**
     *
     * @return ConnectionValidationTimeout
     */
    public int getConnectionValidationTimeout() {
        return delegate.getConnectionValidationTimeout();
    }

    /**
     * <p>Attempts to establish a connection with the data source that
     * this {@code DataSource} object represents.
     *
     * @return a connection to the data source
     * @exception SQLException if a database access error occurs
     * timeout value specified by the {@code setLoginTimeout} method
     * has been exceeded and has at least tried to cancel the
     * current database connection attempt
     */
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    /**
     * <p>Attempts to establish a connection with the data source that
     * this {@code DataSource} object represents.
     *
     * @param username the database user on whose behalf the connection is
     *  being made
     * @param password the user's password
     * @return a connection to the data source
     * @exception SQLException if a database access error occurs
     * timeout value specified by the {@code setLoginTimeout} method
     * has been exceeded and has at least tried to cancel the
     * current database connection attempt
     * @since 1.4
     */
    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    /**
     * {@inheritDoc}
     * @since 1.4
     */
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    /**
     * {@inheritDoc}
     * @since 1.4
     * @param out
     */
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    /**
     * {@inheritDoc}
     * @since 1.4
     * @param seconds
     */
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    /**
     * {@inheritDoc}
     * @since 1.4
     */
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    /**
     * Return the parent Logger of all the Loggers used by this data source. This
     * should be the Logger farthest from the root Logger that is
     * still an ancestor of all of the Loggers used by this data source. Configuring
     * this Logger will affect all of the log messages generated by the data source.
     * In the worst case, this may be the root Logger.
     *
     * @return the parent Logger for this data source
     * @throws SQLFeatureNotSupportedException if the data source does not use
     * {@code java.util.logging}
     * @since 1.7
     */
    public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    /**
     * Returns an object that implements the given interface to allow access to
     * non-standard methods, or standard methods not exposed by the proxy.
     *
     * If the receiver implements the interface then the result is the receiver
     * or a proxy for the receiver. If the receiver is a wrapper
     * and the wrapped object implements the interface then the result is the
     * wrapped object or a proxy for the wrapped object. Otherwise return the
     * the result of calling <code>unwrap</code> recursively on the wrapped object
     * or a proxy for that result. If the receiver is not a
     * wrapper and does not implement the interface, then an <code>SQLException</code> is thrown.
     *
     * @param iface A Class defining an interface that the result must implement.
     * @param <T> This is the type parameter
     * @return an object that implements the interface. May be a proxy for the actual implementing object.
     * @throws SQLException an sql exception If no object found that implements the interface
     * @since 1.6
     */
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    /**
     * Returns true if this either implements the interface argument or is directly or indirectly a wrapper
     * for an object that does. Returns false otherwise. If this implements the interface then return true,
     * else if this is a wrapper then return the result of recursively calling <code>isWrapperFor</code> on the wrapped
     * object. If this does not implement the interface and is not a wrapper, return false.
     * This method should be implemented as a low-cost operation compared to <code>unwrap</code> so that
     * callers can use this method to avoid expensive <code>unwrap</code> calls that may fail. If this method
     * returns true then calling <code>unwrap</code> with the same argument should succeed.
     *
     * @param iface a Class defining an interface.
     * @return true if this implements the interface or directly or indirectly wraps an object that does.
     * @throws SQLException an sql exception  if an error occurs while determining whether this is a wrapper
     * for an object with the given interface.
     * @since 1.6
     */
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
