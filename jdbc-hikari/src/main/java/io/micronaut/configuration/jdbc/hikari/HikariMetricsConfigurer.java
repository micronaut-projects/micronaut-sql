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
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Internal;
import io.micronaut.jdbc.DataSourceResolver;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Adds the {@link MeterRegistry} to each Hikari pool that the {@link DatasourceFactory} creates, as the pool is created,
 * unless the JDBC metrics binder is disabled with {@code micronaut.metrics.binders.jdbc.enabled=false}.
 * It holds the registry, so the factory does not need to.
 * <p>
 * Hikari accepts a registry only once per pool. A pool that already has one, or a metrics tracker factory, is left as
 * it is: development mode runs the listeners again on a pool that it retains across a restart.
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

    /**
     * @param meterRegistry The meter registry
     * @param dataSourceResolver The data source resolver, which unwraps a data source
     * @param enabled Whether the JDBC metrics binder is enabled
     */
    HikariMetricsConfigurer(MeterRegistry meterRegistry,
                            @Nullable DataSourceResolver dataSourceResolver,
                            @Value("${" + MICRONAUT_METRICS_BINDERS + ".jdbc.enabled:true}") boolean enabled) {
        this.meterRegistry = meterRegistry;
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
        this.enabled = enabled;
    }

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        DataSource dataSource = event.getBean();
        if (enabled
                && dataSourceResolver.resolve(dataSource) instanceof HikariUrlDataSource hikariDataSource
                && hikariDataSource.getMetricRegistry() == null
                && hikariDataSource.getMetricsTrackerFactory() == null) {
            hikariDataSource.setMetricRegistry(meterRegistry);
        }
        return dataSource;
    }
}
