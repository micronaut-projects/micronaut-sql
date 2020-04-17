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
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import oracle.ucp.jdbc.PoolDataSourceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

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
     * @param name name that comes from properties
     */
    public DatasourceConfiguration(@Parameter String name) throws SQLException {
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
        return this.username;
    }

    /**
     * @param username the username
     * @throws SQLException an sql exception
     */
    public void setUsername(String username) throws SQLException {
        this.username = username;
        this.delegate.setUser(username);
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

}
