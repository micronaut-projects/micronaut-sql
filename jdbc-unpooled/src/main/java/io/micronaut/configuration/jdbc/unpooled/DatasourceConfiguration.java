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

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Allows the configuration of unpooled JDBC data sources.
 *
 * <p>If the url, driver class, username, or password are missing, sensible defaults
 * will be provided when possible. If no configuration beyond the datasource name
 * is provided, an in-memory datastore will be configured based on the available
 * drivers on the classpath.</p>
 *
 * <p><strong>Warning:</strong> Unpooled datasources create a new connection for each request
 * and have significant performance implications. Use only for testing, serverless, or
 * very low-volume applications. For production use, consider HikariCP or another
 * pooled datasource implementation.</p>
 *
 * @author Micronaut Team
 * @since 6.3.0
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
public class DatasourceConfiguration implements BasicJdbcConfiguration {

    private final CalculatedSettings calculatedSettings;
    private final String name;
    private String configuredUrl;
    private String configuredDriverClassName;
    private String configuredUsername;
    private String configuredPassword;
    private String configuredValidationQuery;
    private int loginTimeout = 0;
    private final Map<String, Object> dataSourceProperties = new HashMap<>();

    /**
     * Constructor.
     *
     * @param name The name of the datasource from configuration
     */
    public DatasourceConfiguration(@Parameter String name) {
        this.name = name;
        this.calculatedSettings = new CalculatedSettings(this);
    }

    /**
     * Calculates default values for url, driver, username, and password if not configured.
     */
    @PostConstruct
    void postConstruct() {
        if (configuredUrl == null) {
            configuredUrl = getUrl();
        }
        if (configuredDriverClassName == null) {
            configuredDriverClassName = getDriverClassName();
        }
        if (configuredUsername == null) {
            configuredUsername = getUsername();
        }
        if (configuredPassword == null) {
            configuredPassword = getPassword();
        }
        if (configuredValidationQuery == null) {
            configuredValidationQuery = getValidationQuery();
        }
    }

    @Override
    @NonNull
    public String getName() {
        return name;
    }

    @Override
    @Nullable
    public String getConfiguredUrl() {
        return configuredUrl;
    }

    @Override
    @NonNull
    public String getUrl() {
        return calculatedSettings.getUrl();
    }

    @Override
    public void setUrl(@Nullable String url) {
        this.configuredUrl = url;
    }

    @Override
    @Nullable
    public String getConfiguredDriverClassName() {
        return configuredDriverClassName;
    }

    @Override
    @Nullable
    public String getDriverClassName() {
        return calculatedSettings.getDriverClassName();
    }

    @Override
    public void setDriverClassName(@Nullable String driverClassName) {
        this.configuredDriverClassName = driverClassName;
    }

    @Override
    @Nullable
    public String getConfiguredUsername() {
        return configuredUsername;
    }

    @Override
    @Nullable
    public String getUsername() {
        return calculatedSettings.getUsername();
    }

    @Override
    public void setUsername(@Nullable String username) {
        this.configuredUsername = username;
    }

    @Override
    @Nullable
    public String getConfiguredPassword() {
        return configuredPassword;
    }

    @Override
    @Nullable
    public String getPassword() {
        return calculatedSettings.getPassword();
    }

    @Override
    public void setPassword(@Nullable String password) {
        this.configuredPassword = password;
    }

    @Override
    @Nullable
    public String getConfiguredValidationQuery() {
        return configuredValidationQuery;
    }

    @Override
    @Nullable
    public String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    /**
     * Sets the validation query.
     *
     * @param validationQuery The validation query
     */
    public void setValidationQuery(@Nullable String validationQuery) {
        this.configuredValidationQuery = validationQuery;
    }

    /**
     * Gets the login timeout in seconds.
     *
     * @return The login timeout
     */
    public int getLoginTimeout() {
        return loginTimeout;
    }

    /**
     * Sets the login timeout in seconds.
     *
     * @param loginTimeout The login timeout
     */
    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    /**
     * Gets the datasource properties.
     *
     * @return The datasource properties as a Properties object
     */
    @NonNull
    public Properties getDataSourcePropertiesAsProperties() {
        Properties props = new Properties();
        props.putAll(dataSourceProperties);
        return props;
    }

    @Override
    public void setDataSourceProperties(@MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.RAW) Map<String, ?> dsProperties) {
        if (dsProperties != null) {
            this.dataSourceProperties.putAll(dsProperties);
        }
    }
}
