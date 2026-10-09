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
package io.micronaut.configuration.jdbc.hikari;

import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Value;
import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Internal;
import io.micronaut.jdbc.DataSourceResolver;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Adds the {@link MeterRegistry} to each Hikari pool that the {@link DatasourceFactory} creates, as the pool is created,
 * unless the JDBC metrics binder is disabled with {@code micronaut.metrics.binders.jdbc.enabled=false}.
 * It holds the registry, so the factory does not need to.
 * <p>
 * Hikari accepts a registry, or a metrics tracker factory, only once per pool. A pool that already has one is left as
 * it is. In development mode, which retains a pool across a restart and runs the listeners again on it, a pool is
 * given a {@link RebindableMetricsTrackerFactory} instead: each context binds its own registry to it, and unbinds it
 * as it stops, so that a retained pool reports to the registry of the current context and keeps none of a previous one.
 *
 * @since 7.3.0
 */
@Internal
@Singleton
@Requires(classes = MeterRegistry.class)
@Requires(beans = MeterRegistry.class)
final class HikariMetricsConfigurer implements BeanCreatedEventListener<DataSource> {

    private final MeterRegistry meterRegistry;
    private final DataSourceResolver dataSourceResolver;
    private final boolean enabled;
    private final boolean developmentMode;
    private final Set<RebindableMetricsTrackerFactory> bound = ConcurrentHashMap.newKeySet();

    /**
     * @param meterRegistry The meter registry
     * @param dataSourceResolver The data source resolver, which unwraps a data source
     * @param enabled Whether the JDBC metrics binder is enabled
     * @param environment The environment, which tells whether development mode is on
     */
    HikariMetricsConfigurer(MeterRegistry meterRegistry,
                            @Nullable DataSourceResolver dataSourceResolver,
                            @Value("${" + MICRONAUT_METRICS_BINDERS + ".jdbc.enabled:true}") boolean enabled,
                            Environment environment) {
        this.meterRegistry = meterRegistry;
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
        this.enabled = enabled;
        this.developmentMode = environment.isDevelopmentMode();
    }

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        DataSource dataSource = event.getBean();
        if (enabled && dataSourceResolver.resolve(dataSource) instanceof HikariUrlDataSource hikariDataSource) {
            if (hikariDataSource.getMetricsTrackerFactory() instanceof RebindableMetricsTrackerFactory trackerFactory) {
                // a pool retained across a development restart: it reports to this context's registry from now on
                trackerFactory.bind(meterRegistry);
                bound.add(trackerFactory);
            } else if (hikariDataSource.getMetricRegistry() == null && hikariDataSource.getMetricsTrackerFactory() == null) {
                if (developmentMode) {
                    RebindableMetricsTrackerFactory trackerFactory = new RebindableMetricsTrackerFactory(meterRegistry);
                    hikariDataSource.setMetricsTrackerFactory(trackerFactory);
                    bound.add(trackerFactory);
                } else {
                    hikariDataSource.setMetricRegistry(meterRegistry);
                }
            }
        }
        return dataSource;
    }

    /**
     * Unbinds the registry of this context from the pools that development mode retains, so that a retained pool
     * neither keeps the registry of a stopped context nor reports to it.
     */
    @PreDestroy
    void unbind() {
        for (RebindableMetricsTrackerFactory trackerFactory : bound) {
            trackerFactory.unbind(meterRegistry);
        }
        bound.clear();
    }
}
