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
package io.micronaut.configuration.hibernate.jpa.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micronaut.configuration.metrics.annotation.RequiresMetrics;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.util.CollectionUtils;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;
import org.hibernate.SessionFactory;
import org.hibernate.stat.HibernateMetrics;

import javax.persistence.EntityManagerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS;

/**
 * Binds metrics for Micrometer for each configured {@link EntityManagerFactory}.
 *
 * @author graemerocher
 * @since 1.0
 */
@Singleton
@RequiresMetrics
@Requires(property = HibernateMetricsBinder.HIBERNATE_METRICS_ENABLED, value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
@Requires(classes = HibernateMetrics.class)
public class HibernateMetricsBinder implements BeanCreatedEventListener<EntityManagerFactory> {

    public static final String HIBERNATE_METRICS_ENABLED = MICRONAUT_METRICS_BINDERS + ".hibernate.enabled";
    private final BeanProvider<MeterRegistry> meterRegistryProvider;
    private final List<Tag> tags;

    /**
     * Default constructor.
     * @param meterRegistryProvider The meter registry provider
     * @param tags The tags
     */
    public HibernateMetricsBinder(
            BeanProvider<MeterRegistry> meterRegistryProvider,
            @Property(name = MICRONAUT_METRICS_BINDERS + ".hibernate.tags")
            @MapFormat(transformation = MapFormat.MapTransformation.FLAT)
            Map<String, String> tags) {
        this.meterRegistryProvider = meterRegistryProvider;
        if (CollectionUtils.isNotEmpty(tags)) {
            this.tags = tags.entrySet().stream().map(entry -> Tag.of(entry.getKey(), entry.getValue())).collect(Collectors.toList());
        } else {
            this.tags = Collections.emptyList();
        }

    }

    @Override
    public EntityManagerFactory onCreated(BeanCreatedEvent<EntityManagerFactory> event) {
        EntityManagerFactory entityManagerFactory = event.getBean();
        String sessionFactoryName = event.getBeanIdentifier().getName();
        MeterRegistry meterRegistry = meterRegistryProvider.get();
        if (entityManagerFactory instanceof SessionFactory) {

            HibernateMetrics.monitor(
                    meterRegistry,
                    (SessionFactory) entityManagerFactory,
                    sessionFactoryName,
                    tags
            );
        }

        return entityManagerFactory;
    }
}
