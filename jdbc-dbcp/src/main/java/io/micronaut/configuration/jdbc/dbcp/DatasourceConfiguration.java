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

import io.micronaut.context.annotation.Context;
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
import org.apache.commons.dbcp2.BasicDataSource;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
@Context
@EachProperty(value = BasicJdbcConfiguration.PREFIX, primary = "default")
public class DatasourceConfiguration extends BasicDataSource implements BasicJdbcConfiguration {

    private static final String ORACLE_VSESSION_PROGRAM = "v$session.program";

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceConfiguration.class);
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
        super();
        this.name = name;
        this.environment = environment;
        this.calculatedSettings = new CalculatedSettings(this);
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
                    this::addConnectionProperty,
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
            this.close();
        } catch (Exception e) {
            if (LOG.isWarnEnabled()) {
                LOG.warn("Error closing data source [" + this + "]: " + e.getMessage(), e);
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
    public String getConfiguredDriverClassName() {
        return super.getDriverClassName();
    }

    @Override
    public String getUrl() {
        return calculatedSettings.getUrl();
    }

    @Override
    public String getConfiguredUrl() {
        return super.getUrl();
    }

    @Override
    public @Nullable String getUsername() {
        return calculatedSettings.getUsername();
    }

    @Override
    public String getConfiguredUsername() {
        return super.getUsername();
    }

    @Override
    public @Nullable String getPassword() {
        return calculatedSettings.getPassword();
    }

    @Override
    public String getConfiguredPassword() {
        return super.getPassword();
    }

    @Override
    public @Nullable String getValidationQuery() {
        return calculatedSettings.getValidationQuery();
    }

    /**
     * A helper method to allow setting the connectionProperties via a single String.
     *
     * @param connectionProperties The connection properties
     */
    public void setConnectionPropertiesString(@Property(name = "datasources.*.connection-properties") String connectionProperties) {
        setConnectionProperties(connectionProperties);
    }

    @Override
    public void setDataSourceProperties(@MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.RAW)  Map<String, ?> dsProperties) {
        if (dsProperties != null) {
            dsProperties.forEach((s, o) -> {
                if (o != null) {
                    if (ORACLE_VSESSION_PROGRAM.equalsIgnoreCase(s)) {
                        oracleProgramProvided = true;
                    }
                    addConnectionProperty(s, o.toString());
                }
            });
        }
    }

    @Override
    public String getConfiguredValidationQuery() {
        return super.getValidationQuery();
    }

    /**
     * Sets an indicator telling whether data source is enabled.
     * If enabled is false, that means datasource is disabled and this method will throw
     * {@link DisabledBeanException} thus preventing this datasource from being added to the context.
     *
     * @param enabled an indicator telling whether data source is enabled
     */
    @Internal
    void setEnabled(boolean enabled) {
        if (!enabled) {
            // This is the only way to disable this bean which is actual datasource
            // because dbcp doesn't have datasource factory like other datasource implementations
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
