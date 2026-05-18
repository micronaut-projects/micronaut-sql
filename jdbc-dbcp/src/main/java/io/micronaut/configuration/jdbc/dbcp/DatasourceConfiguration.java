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
package io.micronaut.configuration.jdbc.dbcp;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.env.Environment;
import io.micronaut.context.exceptions.DisabledBeanException;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import io.micronaut.jdbc.CalculatedSettings;
import io.micronaut.jdbc.OracleSessionProgramHelper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Allows the configuration of Apache DBCP JDBC data sources. All properties on
 * {@link BasicDataSource} are available to be configured.
 *
 * If the url, driver class, username, or password are missing, sensible defaults
 * will be provided when possible. If no configuration beyond the datasource name
 * is provided, an in memory datastore will be configured based on the available
 * drivers on the classpath.
 *
 * @author James Kleeh
 * @since 1.0
 */
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
public class DatasourceConfiguration implements BasicJdbcConfiguration {

    private static final String ORACLE_VSESSION_PROGRAM = "v$session.program";

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceConfiguration.class);

    @ConfigurationBuilder(
            allowZeroArgs = true,
            excludes = {"connectionProperties", "driverClassName", "url", "username", "password", "validationQuery"}
    )
    private final BasicDataSource delegate = new BasicDataSource();

    private final CalculatedSettings calculatedSettings;
    private final String name;
    private final Environment environment;
    private boolean oracleProgramProvided;

    /**
     * Constructor.
     * @param name name configured from properties
     * @param environment The environment
     */
    public DatasourceConfiguration(@Parameter String name, Environment environment) {
        this.name = name;
        this.environment = environment;
        this.calculatedSettings = new CalculatedSettings(this);
    }

    /**
     * Returns the configured DBCP datasource.
     */
    public BasicDataSource getBasicDataSource() {
        return delegate;
    }

    /**
     * Returns the configuration builder delegate.
     */
    public BasicDataSource getDelegate() {
        return delegate;
    }

    /**
     * Apache DBCP uses the fields instead of using getters to create a
     * connection, so the following is required to populate the calculated
     * values into the fields.
     */
    @PostConstruct
    void postConstruct() {
        if (getConfiguredUrl() == null) {
            setUrl(getUrl());
        }
        if (getConfiguredDriverClassName() == null) {
            setDriverClassName(getDriverClassName());
        }
        if (getConfiguredUsername() == null) {
            setUsername(getUsername());
        }
        if (getConfiguredPassword() == null) {
            setPassword(getPassword());
        }
        if (getConfiguredValidationQuery() == null) {
            setValidationQuery(getValidationQuery());
        }
        try {
            boolean provided = OracleSessionProgramHelper.apply(
                    getName(),
                    getUrl(),
                    environment.getProperty("datasources." + getName() + ".dialect", String.class).orElse(null),
                    environment,
                    delegate::addConnectionProperty,
                    () -> oracleProgramProvided
            );
            if (provided) {
                oracleProgramProvided = true;
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping Oracle session program auto-config due to: {}", e.getMessage());
            }
        }
    }

    /**
     * Before this bean is destroyed close the connection.
     */
    @PreDestroy
    void preDestroy() {
        try {
            delegate.close();
        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Error closing data source [{}]: {}", delegate, e.getMessage(), e);
            }
        }
    }

    /**
     * Get the name of the bean.
     * @return name
     */
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getDriverClassName() {
        return calculatedSettings.getDriverClassName();
    }

    @Override
    public void setDriverClassName(String driverClassName) {
        delegate.setDriverClassName(driverClassName);
    }

    @Override
    public String getConfiguredDriverClassName() {
        return delegate.getDriverClassName();
    }

    @Override
    public String getUrl() {
        return calculatedSettings.getUrl();
    }

    @Override
    public void setUrl(String url) {
        delegate.setUrl(url);
    }

    @Override
    public String getConfiguredUrl() {
        return delegate.getUrl();
    }

    @Override
    public @Nullable String getUsername() {
        return calculatedSettings.getUsername();
    }

    @Override
    public void setUsername(@Nullable String username) {
        delegate.setUsername(username);
    }

    @Override
    public String getConfiguredUsername() {
        return delegate.getUsername();
    }

    @Override
    public @Nullable String getPassword() {
        return calculatedSettings.getPassword();
    }

    @Override
    public void setPassword(@Nullable String password) {
        delegate.setPassword(password);
    }

    @Override
    public String getConfiguredPassword() {
        return delegate.getPassword();
    }

    @Override
    public @Nullable String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    /**
     * Setter for validation query.
     *
     * @param validationQuery The validation query
     */
    public void setValidationQuery(@Nullable String validationQuery) {
        delegate.setValidationQuery(validationQuery);
    }

    /**
     * Sets the maximum wait time in milliseconds.
     *
     * @param maxWaitMillis The maximum wait time in milliseconds
     */
    public void setMaxWaitMillis(long maxWaitMillis) {
        delegate.setMaxWaitMillis(maxWaitMillis);
    }

    /**
     * @return The configured maximum wait time in milliseconds
     */
    public long getMaxWaitMillis() {
        return delegate.getMaxWaitMillis();
    }

    /**
     * Sets the time between eviction runs in milliseconds.
     *
     * @param timeBetweenEvictionRunsMillis The time between eviction runs in milliseconds
     */
    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        delegate.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
    }

    /**
     * @return The configured time between eviction runs in milliseconds
     */
    public long getTimeBetweenEvictionRunsMillis() {
        return delegate.getTimeBetweenEvictionRunsMillis();
    }

    /**
     * Sets the minimum evictable idle time in milliseconds.
     *
     * @param minEvictableIdleTimeMillis The minimum evictable idle time in milliseconds
     */
    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        delegate.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
    }

    /**
     * @return The configured minimum evictable idle time in milliseconds
     */
    public long getMinEvictableIdleTimeMillis() {
        return delegate.getMinEvictableIdleTimeMillis();
    }

    /**
     * Sets the soft minimum evictable idle time in milliseconds.
     *
     * @param softMinEvictableIdleTimeMillis The soft minimum evictable idle time in milliseconds
     */
    public void setSoftMinEvictableIdleTimeMillis(long softMinEvictableIdleTimeMillis) {
        delegate.setSoftMinEvictableIdleTimeMillis(softMinEvictableIdleTimeMillis);
    }

    /**
     * @return The configured soft minimum evictable idle time in milliseconds
     */
    public long getSoftMinEvictableIdleTimeMillis() {
        return delegate.getSoftMinEvictableIdleTimeMillis();
    }

    /**
     * Sets the maximum connection lifetime in milliseconds.
     *
     * @param maxConnLifetimeMillis The maximum connection lifetime in milliseconds
     */
    public void setMaxConnLifetimeMillis(long maxConnLifetimeMillis) {
        delegate.setMaxConnLifetimeMillis(maxConnLifetimeMillis);
    }

    /**
     * @return The configured maximum connection lifetime in milliseconds
     */
    public long getMaxConnLifetimeMillis() {
        return delegate.getMaxConnLifetimeMillis();
    }

    /**
     * Sets the remove abandoned timeout in seconds.
     *
     * @param removeAbandonedTimeout The remove abandoned timeout in seconds
     */
    public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
        delegate.setRemoveAbandonedTimeout(removeAbandonedTimeout);
    }

    /**
     * @return The configured remove abandoned timeout in seconds
     */
    public int getRemoveAbandonedTimeout() {
        return delegate.getRemoveAbandonedTimeout();
    }

    /**
     * Sets the validation query timeout in seconds.
     *
     * @param validationQueryTimeout The validation query timeout in seconds
     */
    public void setValidationQueryTimeout(int validationQueryTimeout) {
        delegate.setValidationQueryTimeout(validationQueryTimeout);
    }

    /**
     * @return The configured validation query timeout in seconds
     */
    public int getValidationQueryTimeout() {
        return delegate.getValidationQueryTimeout();
    }

    /**
     * Sets the default query timeout in seconds.
     *
     * @param defaultQueryTimeout The default query timeout in seconds
     */
    public void setDefaultQueryTimeout(Integer defaultQueryTimeout) {
        delegate.setDefaultQueryTimeout(defaultQueryTimeout);
    }

    /**
     * @return The configured default query timeout in seconds
     */
    public Integer getDefaultQueryTimeout() {
        return delegate.getDefaultQueryTimeout();
    }

    @Override
    public String getConfiguredValidationQuery() {
        return delegate.getValidationQuery();
    }

    /**
     * Sets the connection properties.
     *
     * @param connectionProperties The connection properties
     */
    public void setConnectionProperties(String connectionProperties) {
        delegate.setConnectionProperties(connectionProperties);
    }

    @Override
    public void setDataSourceProperties(@MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.RAW) Map<String, ?> dsProperties) {
        if (dsProperties != null) {
            dsProperties.forEach((s, o) -> {
                if (o != null) {
                    if (ORACLE_VSESSION_PROGRAM.equalsIgnoreCase(s)) {
                        oracleProgramProvided = true;
                    }
                    delegate.addConnectionProperty(s, o.toString());
                }
            });
        }
    }

    /**
     * Sets an indicator telling whether data source is enabled.
     * If enabled is false, that means datasource is disabled and this method will throw
     * {@link DisabledBeanException} thus preventing this datasource configuration from being added to the context.
     *
     * @param enabled an indicator telling whether data source is enabled
     */
    @Internal
    void setEnabled(boolean enabled) {
        if (!enabled) {
            throw new DisabledBeanException("The datasource \"" + name + "\" is disabled");
        }
    }

    /**
     * Checks if the Oracle program has been provided.
     *
     * The Oracle program is considered provided if it has been explicitly set
     * through the 'datasources.*.data-source-properties' or 'datasources.*.oracle.session.enabled'
     * configuration properties (using Micronaut Application as value).
     * Currently used for testing purposes.
     *
     * @return true if the Oracle program has been provided, false otherwise
     */
    public boolean isOracleProgramProvided() {
        return oracleProgramProvided;
    }
}
