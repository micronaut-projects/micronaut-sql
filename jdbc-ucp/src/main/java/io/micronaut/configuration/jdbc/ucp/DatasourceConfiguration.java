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
import oracle.ucp.jdbc.PoolDataSource;

/**
 * Allows the configuration of UCP JDBC data sources. All properties on
 * {@link PoolDataSource} are available to be configured.
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

    public CalculatedSettings calculatedSettings;
    private String name;
    private String connectionFactoryClassName = "oracle.jdbc.pool.OracleDataSource";
    private String driverClassName;
    private String connectionPoolName;
    private String url;
    private String username;
    private String password;
    private Integer initialPoolSize = 5;
    private Integer minPoolSize = 5;
    private Integer maxPoolSize = 20;
    private Integer timeoutCheckInterval = 5;
    private Integer inactiveConnectionTimeout = 10;

    private Integer connectionWaitTimeout;
    private Integer loginTimeout;
    private Integer maxConnectionReuseCount;
    private Integer maxConnectionReuseTime;
    private Integer maxIdleTime;
    private Integer maxStatements;
    private String networkProtocol;
    private String onsConfiguration;
    private Integer portNumber;
    private Integer propertyCycle;
    private String roleName;
    private String serverName;
    private String sqlForValidateConnection;
    private Boolean fastConnectionFailoverEnabled;
    private Boolean validateConnectionOnBorrow;

    private Boolean fixedString = false;
    private Boolean remarksReporting = false;
    private Boolean restrictGetTables = false;
    private Boolean includeSynonyms = false;
    private Boolean defaultNChar = false;
    private Boolean accumulateBatchResult = false;

    /**
     * Constructor.
     * @param name name that comes from properties
     */
    public DatasourceConfiguration(@Parameter String name) {
        this.name = name;
        this.calculatedSettings = new CalculatedSettings(this);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getMaxConnectionReuseTime() {
        return maxConnectionReuseTime;
    }

    public void setMaxConnectionReuseTime(Integer maxConnectionReuseTime) {
        this.maxConnectionReuseTime = maxConnectionReuseTime;
    }

    public Integer getConnectionWaitTimeout() {
        return connectionWaitTimeout;
    }

    public void setConnectionWaitTimeout(Integer connectionWaitTimeout) {
        this.connectionWaitTimeout = connectionWaitTimeout;
    }

    public Integer getLoginTimeout() {
        return loginTimeout;
    }

    public void setLoginTimeout(Integer loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    public Integer getMaxConnectionReuseCount() {
        return maxConnectionReuseCount;
    }

    public void setMaxConnectionReuseCount(Integer maxConnectionReuseCount) {
        this.maxConnectionReuseCount = maxConnectionReuseCount;
    }

    public Integer getMaxIdleTime() {
        return maxIdleTime;
    }

    public void setMaxIdleTime(Integer maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }

    public Integer getMaxStatements() {
        return maxStatements;
    }

    public void setMaxStatements(Integer maxStatements) {
        this.maxStatements = maxStatements;
    }

    public String getNetworkProtocol() {
        return networkProtocol;
    }

    public void setNetworkProtocol(String networkProtocol) {
        this.networkProtocol = networkProtocol;
    }

    public String getOnsConfiguration() {
        return onsConfiguration;
    }

    public void setOnsConfiguration(String onsConfiguration) {
        this.onsConfiguration = onsConfiguration;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    public Integer getPropertyCycle() {
        return propertyCycle;
    }

    public void setPropertyCycle(Integer propertyCycle) {
        this.propertyCycle = propertyCycle;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getSqlForValidateConnection() {
        return sqlForValidateConnection;
    }

    public void setSqlForValidateConnection(String sqlForValidateConnection) {
        this.sqlForValidateConnection = sqlForValidateConnection;
    }

    public Boolean isFastConnectionFailoverEnabled() {
        return fastConnectionFailoverEnabled;
    }

    public void setFastConnectionFailoverEnabled(Boolean fastConnectionFailoverEnabled) {
        this.fastConnectionFailoverEnabled = fastConnectionFailoverEnabled;
    }

    public Boolean isValidateConnectionOnBorrow() {
        return validateConnectionOnBorrow;
    }

    public void setValidateConnectionOnBorrow(Boolean validateConnectionOnBorrow) {
        this.validateConnectionOnBorrow = validateConnectionOnBorrow;
    }

    public String getConnectionFactoryClassName() {
        return connectionFactoryClassName;
    }

    public void setConnectionFactoryClassName(String connectionFactoryClassName) {
        this.connectionFactoryClassName = connectionFactoryClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getConnectionPoolName() {
        return connectionPoolName;
    }

    public void setConnectionPoolName(String connectionPoolName) {
        this.connectionPoolName = connectionPoolName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(Integer minPoolSize) {
        this.minPoolSize = minPoolSize;
    }

    public Integer getMaxPoolSize() {
        return maxPoolSize;
    }

    public void setMaxPoolSize(Integer maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    public Integer getTimeoutCheckInterval() {
        return timeoutCheckInterval;
    }

    public void setTimeoutCheckInterval(Integer timeoutCheckInterval) {
        this.timeoutCheckInterval = timeoutCheckInterval;
    }

    public Integer getInactiveConnectionTimeout() {
        return inactiveConnectionTimeout;
    }

    public void setInactiveConnectionTimeout(Integer inactiveConnectionTimeout) {
        this.inactiveConnectionTimeout = inactiveConnectionTimeout;
    }

    public Boolean isFixedString() {
        return fixedString;
    }

    public void setFixedString(Boolean fixedString) {
        this.fixedString = fixedString;
    }

    public Boolean isRemarksReporting() {
        return remarksReporting;
    }

    public void setRemarksReporting(Boolean remarksReporting) {
        this.remarksReporting = remarksReporting;
    }

    public Boolean isRestrictGetTables() {
        return restrictGetTables;
    }

    public void setRestrictGetTables(Boolean restrictGetTables) {
        this.restrictGetTables = restrictGetTables;
    }

    public Boolean isIncludeSynonyms() {
        return includeSynonyms;
    }

    public void setIncludeSynonyms(Boolean includeSynonyms) {
        this.includeSynonyms = includeSynonyms;
    }

    public Boolean isDefaultNChar() {
        return defaultNChar;
    }

    public void setDefaultNChar(Boolean defaultNChar) {
        this.defaultNChar = defaultNChar;
    }

    public Boolean isAccumulateBatchResult() {
        return accumulateBatchResult;
    }

    public void setAccumulateBatchResult(Boolean accumulateBatchResult) {
        this.accumulateBatchResult = accumulateBatchResult;
    }

    public Integer getInitialPoolSize() {
        return initialPoolSize;
    }

    public void setInitialPoolSize(Integer initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }

    public CalculatedSettings getCalculatedSettings() {
        return calculatedSettings;
    }

    public void setCalculatedSettings(CalculatedSettings calculatedSettings) {
        this.calculatedSettings = calculatedSettings;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDriverClassName() {
        return calculatedSettings.getDriverClassName();
    }

    @Override
    public String getConfiguredDriverClassName() {
        return this.driverClassName;
    }

    @Override
    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getConfiguredUrl() {
        return this.url;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public String getConfiguredUsername() {
        return this.username;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getConfiguredPassword() {
        return this.password;
    }

    @Override
    public String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    @Override
    public String getConfiguredValidationQuery() {
        return this.getValidationQuery();
    }
}
