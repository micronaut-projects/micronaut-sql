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
import io.micronaut.core.util.StringUtils;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;
import jakarta.annotation.PostConstruct;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;
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
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceConfiguration.class);

    @ConfigurationBuilder(allowZeroArgs = true, excludes = {"connectionFactoryProperties"})
    PoolDataSourceImpl delegate = (PoolDataSourceImpl) PoolDataSourceFactory.getPoolDataSource();
    private CalculatedSettings calculatedSettings;
    private String name;
    private String username;

    /**
     * Constructor.
     *
     * @param name name that comes from properties
     */
    public DatasourceConfiguration(@Parameter String name) throws SQLException {
        super();
        this.name = name;
        this.delegate.setConnectionPoolName(name);
        this.calculatedSettings = new CalculatedSettings(this);
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
            this.delegate.setURL(url);
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
        this.username = username;
        try {
            this.delegate.setUser(username);
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
            this.delegate.setPassword(password);
        } catch (SQLException e) {
            throw new ConfigurationException("Unable to set datasource password: " + e.getMessage(), e);
        }

    }

    @Override
    public String getConfiguredPassword() {
        return delegate.getPassword();
    }

    @Override
    public String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

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
            Properties properties = new Properties();
            dsProperties.forEach((key, value) -> {
                if (value != null) {
                    properties.put(key, value.toString());
                }
            });

            try {
                this.delegate.setConnectionProperties(properties);
            } catch (SQLException e) {
                throw new ConfigurationException("Unable to set datasource properties: " + e.getMessage(), e);
            }
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
    }
}
