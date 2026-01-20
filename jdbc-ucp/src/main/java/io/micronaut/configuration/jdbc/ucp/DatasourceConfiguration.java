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

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.context.env.Environment;
import io.micronaut.core.util.StringUtils;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;
import jakarta.annotation.PostConstruct;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Allows the configuration of UCP JDBC data sources. All properties on
 * {@link PoolDataSourceImpl} are available to be configured.
 * <p>
 * If the url, driver class, validation sql query, username, or password are missing, sensible defaults
 * will be provided when possible. If no configuration beyond the datasource name
 * is provided, an in memory datasource will be configured based on the available
 * drivers on the classpath.
 *
 * @author toddsharp
 * @since 2.0.1
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
@Context
public class DatasourceConfiguration implements BasicJdbcConfiguration {

    private static final String ORACLE_VSESSION_PROGRAM = "v$session.program";
    private static final String DATASOURCES_PREFIX = "datasources.";

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceConfiguration.class);

    @ConfigurationBuilder(allowZeroArgs = true, excludes = {"connectionFactoryProperties", "URL", "username", "password"})
    PoolDataSourceImpl delegate = (PoolDataSourceImpl) PoolDataSourceFactory.getPoolDataSource();
    private CalculatedSettings calculatedSettings;
    private String name;
    private String username;
    private String password;
    private Properties dataSourceProperties = new Properties();

    /**
     * Constructor.
     *
     * @param name name that comes from properties
     */
    private final Environment environment;

    public DatasourceConfiguration(@Parameter String name, Environment environment) throws SQLException {
        super();
        this.name = name;
        this.environment = environment;
        this.delegate.setConnectionPoolName(name);
        this.calculatedSettings = new CalculatedSettings(this);
    }

    /**
     * @return the pool data source
     * @since 4.1
     */
    public PoolDataSource getPoolDataSource() {
        return delegate;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
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
    public void setDriverClassName(String driverClassName) {
        try {
            this.delegate.setConnectionFactoryClassName(driverClassName);
        } catch (SQLException e) {
            throw new ConfigurationException("Unable to set driver class name: " + e.getMessage(), e);
        }
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
    public void setUrl(String url) {
        try {
            if (!Objects.equals(url, this.delegate.getURL())) {
                this.delegate.setURL(url);
            }
        } catch (SQLException e) {
            throw new ConfigurationException("Unable to set datasource URL: " + e.getMessage(), e);
        }
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    /**
     * @param username the username
     */
    public void setUsername(String username) {
        try {
            if (!Objects.equals(this.username, username)) {
                this.username = username;
                this.delegate.setUser(username);
            }
        } catch (SQLException e) {
            throw new ConfigurationException("Unable to set datasource username: " + e.getMessage(), e);
        }
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
    public void setPassword(String password) {
        try {
            if (!Objects.equals(this.password, password)) {
                this.password = password;
                this.delegate.setPassword(password);
            }
        } catch (SQLException e) {
            throw new ConfigurationException("Unable to set datasource password: " + e.getMessage(), e);
        }
    }

    @Override
    public String getConfiguredPassword() {
        return this.password;
    }

    @Override
    public String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    /**
     * Sets the validation query.
     *
     * @param validationQuery The validation query
     */
    public void setValidationQuery(String validationQuery) {
        try {
            delegate.setSQLForValidateConnection(validationQuery);
        } catch (SQLException e) {
            throw new ConfigurationException("Unable to set datasource validation query:" + e.getMessage(), e);
        }
    }

    @Override
    public void setDataSourceProperties(Map<String, ?> dsProperties) {
        if (dsProperties != null) {
            dsProperties.forEach((key, value) -> {
                if (value != null) {
                    dataSourceProperties.put(key, value.toString());
                }
            });
        }
    }

    @Override
    public String getConfiguredValidationQuery() {
        return delegate.getSQLForValidateConnection();
    }

    /**
     * Configures the missing properties of the data source from the calculated settings.
     *
     * @since 4.0.2
     */
    @PostConstruct
    public void initialize() {
        if (StringUtils.isEmpty(getConfiguredDriverClassName()) && !StringUtils.isEmpty(getDriverClassName())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Configuring calculated driver class name: {}", getDriverClassName());
            }
            setDriverClassName(getDriverClassName());
        }

        if (StringUtils.isEmpty(getConfiguredUrl())) {
            String url = null;
            try {
                url = getUrl();
            } catch (ConfigurationException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Failed to configure calculated url: {}", e.getMessage());
                }
            }

            if (url != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Configuring calculated url: {}", getUrl());
                }
                setUrl(url);
            }
        }

        if (StringUtils.isEmpty(getConfiguredValidationQuery())) {
            String validationQuery = null;
            try {
                validationQuery = getValidationQuery();
            } catch (ConfigurationException e) {
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Failed to configure SQL validation query: {}", e.getMessage());
                }
            }

            if (validationQuery != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Configuring calculated SQL validation query: {}", getValidationQuery());
                }
                setValidationQuery(validationQuery);
            }
        }

        if (StringUtils.isEmpty(getConfiguredUsername()) && !StringUtils.isEmpty(calculatedSettings.getUsername())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Configuring calculated username: {}", calculatedSettings.getUsername());
            }
            setUsername(calculatedSettings.getUsername());
        }

        if (StringUtils.isEmpty(getConfiguredPassword())) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Configuring calculated password: *****");
            }
            setPassword(getPassword());
        }

        try {
            applyOracleSessionProgram();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping Oracle session program auto-config due to: {}", e.getMessage());
            }
        }
        if (!dataSourceProperties.isEmpty()) {
            try {
                this.delegate.setConnectionProperties(dataSourceProperties);
            } catch (SQLException e) {
                throw new ConfigurationException("Unable to set datasource properties: " + e.getMessage(), e);
            }
        }
    }

    private void applyOracleSessionProgram() {
        if (dataSourceProperties.containsKey(ORACLE_VSESSION_PROGRAM)) {
            return;
        }
        String dsName = getName();
        String url = getConfiguredUrl();
        String dialect = environment.getProperty(DATASOURCES_PREFIX + dsName + ".dialect", String.class).orElse(null);
        boolean isOracle = (dialect != null && "oracle".equalsIgnoreCase(dialect)) || (url != null && url.toLowerCase().startsWith("jdbc:oracle"));
        if (!isOracle) {
            return;
        }
        boolean enabled = environment.getProperty(DATASOURCES_PREFIX + dsName + ".oracle.session.enabled", boolean.class).orElse(true);
        if (!enabled) {
            return;
        }
        String envOverride = environment.getProperty(DATASOURCES_PREFIX + dsName + ".data-source-properties." + ORACLE_VSESSION_PROGRAM, String.class).orElse(null);
        if (envOverride != null) {
            dataSourceProperties.put(ORACLE_VSESSION_PROGRAM, envOverride);
            return;
        }
        String program = environment.getProperty("micronaut.application.name", String.class).orElse(null);
        if (program == null) {
            return;
        }
        dataSourceProperties.put(ORACLE_VSESSION_PROGRAM, program);
    }
}
