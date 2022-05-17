/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.configuration.hibernate.jpa.conf.settings.internal;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.settings.SettingsSupplier;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import java.util.Collections;
import java.util.Map;

/**
 * Micronaut bean container setting supplier.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Prototype
final class MicronautContainerSettingsSupplier implements SettingsSupplier {

    private final ApplicationContext applicationContext;

    MicronautContainerSettingsSupplier(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public Map<String, Object> supply(JpaConfiguration jpaConfiguration) {
        BeanContainer beanContainer = new BeanContainer() {
            @Override
            public <B> ContainedBean<B> getBean(Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {
                B bean = applicationContext.findBean(beanType)
                        .orElseGet(() -> fallbackProducer.produceBeanInstance(beanType));
                return () -> bean;
            }

            @Override
            public <B> ContainedBean<B> getBean(
                    String name,
                    Class<B> beanType,
                    LifecycleOptions lifecycleOptions,
                    BeanInstanceProducer fallbackProducer) {
                B bean = applicationContext.findBean(beanType, Qualifiers.byName(name))
                        .orElseGet(() -> fallbackProducer.produceBeanInstance(name, beanType));
                return () -> bean;
            }

            @Override
            public void stop() {
                // no-op, managed externally
            }
        };
        return Collections.singletonMap(AvailableSettings.BEAN_CONTAINER, beanContainer);
    }
}
