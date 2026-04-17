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

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;

import jakarta.annotation.PostConstruct;
import java.sql.Driver;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Allows configuration of an unpooled JDBC datasource backed by {@link java.sql.DriverManager}.
 *
 * @author Micronaut
 * @since 7.0.0
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
public class DatasourceConfiguration implements BasicJdbcConfiguration {

    private final CalculatedSettings calculatedSettings;
    private final String name;
    private final Map<String, String> dataSourceProperties = new LinkedHashMap<>(2);

    private String url;
    private Class<? extends Driver> driverClass;
    private String driverClassName;
    private String username;
    private String password;
    private String validationQuery;
    private Integer loginTimeout;

    /**
     * Constructor.
     *
     * @param name The datasource name
     */
    public DatasourceConfiguration(@Parameter String name) {
        this.name = name;
        this.calculatedSettings = new CalculatedSettings(this);
    }

    @PostConstruct
    final void postConstruct() {
        if (url == null) {
            url = getUrl();
        }
        if (driverClassName == null) {
            driverClassName = getDriverClassName();
        }
        if (username == null) {
            username = getUsername();
        }
        if (password == null) {
            password = getPassword();
        }
        if (validationQuery == null) {
            validationQuery = getValidationQuery();
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getConfiguredUrl() {
        return url;
    }

    @Override
    public String getUrl() {
        return calculatedSettings.getUrl();
    }

    @Override
    public void setUrl(String url) {
        this.url = url;
    }

    @Override
    public String getConfiguredDriverClassName() {
        if (driverClass != null) {
            return driverClass.getName();
        }
        return driverClassName;
    }

    @Override
    public String getDriverClassName() {
        return calculatedSettings.getDriverClassName();
    }

    @Override
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    /**
     * @return The configured driver class
     */
    public Class<? extends Driver> getDriverClass() {
        return driverClass;
    }

    /**
     * @param driverClass The configured driver class
     */
    public void setDriverClass(Class<? extends Driver> driverClass) {
        this.driverClass = driverClass;
        this.driverClassName = driverClass == null ? null : driverClass.getName();
    }

    @Override
    public String getConfiguredUsername() {
        return username;
    }

    @Override
    public String getUsername() {
        return calculatedSettings.getUsername();
    }

    @Override
    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String getConfiguredPassword() {
        return password;
    }

    @Override
    public String getPassword() {
        return calculatedSettings.getPassword();
    }

    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String getConfiguredValidationQuery() {
        return validationQuery;
    }

    @Override
    public String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    /**
     * @param validationQuery The configured validation query
     */
    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    /**
     * @return The configured login timeout in seconds
     */
    public Integer getLoginTimeout() {
        return loginTimeout;
    }

    /**
     * @param loginTimeout The login timeout in seconds
     */
    public void setLoginTimeout(Integer loginTimeout) {
        this.loginTimeout = loginTimeout;
    }

    /**
     * @return Additional datasource properties passed to the JDBC driver
     */
    public Map<String, String> getDataSourceProperties() {
        return dataSourceProperties;
    }

    @Override
    public void setDataSourceProperties(@MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.RAW) Map<String, ?> dsProperties) {
        dataSourceProperties.clear();
        if (dsProperties != null) {
            dsProperties.forEach((key, value) -> {
                if (value != null) {
                    dataSourceProperties.put(key, value.toString());
                }
            });
        }
    }
}
