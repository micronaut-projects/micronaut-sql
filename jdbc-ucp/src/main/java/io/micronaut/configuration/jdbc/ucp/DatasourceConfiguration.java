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
import oracle.ucp.jdbc.PoolDataSourceImpl;

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
public class DatasourceConfiguration extends PoolDataSourceImpl implements BasicJdbcConfiguration {

    public CalculatedSettings calculatedSettings;
    private String name;

    /**
     * Constructor.
     * @param name name that comes from properties
     */
    public DatasourceConfiguration(@Parameter String name) {
        this.name = name;
        this.calculatedSettings = new CalculatedSettings(this);
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
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getDriverClassName() {
        return calculatedSettings.getDriverClassName();
    }

    @Override
    public String getConfiguredDriverClassName() {
        return super.getConnectionFactoryClassName();
    }

    @Override
    public String getConfiguredUrl() {
        return super.getURL();
    }

    @Override
    public String getUrl() {
        return calculatedSettings.getUrl();
    }

    @Override
    public String getUsername() {
        return calculatedSettings.getUsername();
    }

    @Override
    public String getConfiguredUsername() {
        return super.getUser();
    }

    @Override
    public String getPassword() {
        return calculatedSettings.getPassword();
    }

    @Override
    public String getConfiguredPassword() {
        return super.getPassword();
    }

    @Override
    public String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    @Override
    public String getConfiguredValidationQuery() {
        return super.getSQLForValidateConnection();
    }
}
