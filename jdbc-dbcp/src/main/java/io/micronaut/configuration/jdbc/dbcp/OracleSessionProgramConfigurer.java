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
package io.micronaut.configuration.jdbc.dbcp;

import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanInitializedEventListener;
import io.micronaut.context.event.BeanInitializingEvent;
import io.micronaut.core.annotation.Internal;
import io.micronaut.jdbc.OracleSessionProgramHelper;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies the Oracle session program to the connection properties of each datasource configuration, which is the
 * pool, as it is created and before the pool opens a connection. It holds the environment, so the pool does not
 * need to.
 * <p>
 * It runs when the configuration is initialized, as the configuration's {@code @PostConstruct} used to, and so before
 * any listener of bean creation, one of which may replace the configuration with a {@link javax.sql.DataSource} wrapper
 * whatever its order. A configuration that development mode retains already has it.
 *
 * @since 7.3.0
 */
@Internal
@Singleton
final class OracleSessionProgramConfigurer implements BeanInitializedEventListener<DatasourceConfiguration> {

    private static final Logger LOG = LoggerFactory.getLogger(OracleSessionProgramConfigurer.class);

    private final Environment environment;

    /**
     * @param environment The environment
     */
    OracleSessionProgramConfigurer(Environment environment) {
        this.environment = environment;
    }

    @Override
    public DatasourceConfiguration onInitialized(BeanInitializingEvent<DatasourceConfiguration> event) {
        DatasourceConfiguration datasourceConfiguration = event.getBean();
        try {
            OracleSessionProgramHelper.apply(
                    datasourceConfiguration.getName(),
                    datasourceConfiguration.getUrl(),
                    environment.getProperty("datasources." + datasourceConfiguration.getName() + ".dialect", String.class).orElse(null),
                    environment,
                    datasourceConfiguration::addOracleProgram,
                    datasourceConfiguration::isOracleProgramProvided
            );
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Skipping Oracle session program auto-config due to: {}", e.getMessage());
            }
        }
        return datasourceConfiguration;
    }
}
