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
package io.micronaut.jdbc;


import java.util.Map;

/**
 * A contract for data source configuration classes to implement that allows for the calculation of several
 * properties based on other properties.
 *
 * @author James Kleeh
 * @author graemerocher
 * @since 1.0
 */
public interface BasicJdbcConfiguration {

    /**
     * The prefix used for data source configuration.
     */
    String PREFIX = "datasources";

    /**
     * @return A user provided name to identify the datasource
     */
    String getName();

    /**
     * @return The URL supplied via configuration
     */
    String getConfiguredUrl();

    /**
     * @return The URL to be used by the data source
     */
    String getUrl();

    /**
     * @param url Sets the url
     * @since 2.1
     */
    void setUrl(String url);

    /**
     * @return The driver class name supplied via configuration
     */
    String getConfiguredDriverClassName();

    /**
     * @return The driver class name to be used by the data source
     */
    String getDriverClassName();

    /**
     * @param driverClassName Sets the driver class name
     * @since 2.1
     */
    void setDriverClassName(String driverClassName);

    /**
     * @return The username supplied via configuration
     */
    String getConfiguredUsername();

    /**
     * @return The username to be used by the data source
     */
    String getUsername();

    /**
     * @param username Sets the username
     * @since 2.1
     */
    void setUsername(String username);

    /**
     * @return The password supplied via configuration
     */
    String getConfiguredPassword();

    /**
     * @return The password to be used by the data source
     */
    String getPassword();

    /**
     * @param password Sets the password
     */
    void setPassword(String password);

    /**
     * @return The validation query supplied via configuration
     */
    String getConfiguredValidationQuery();

    /**
     * @return The validation query to be used by the data source
     */
    String getValidationQuery();

    /**
     * Sets the datasource properties.
     * @param dsProperties The properties
     * @since 2.1
     */
    void setDataSourceProperties(Map<String, ?> dsProperties);

}
