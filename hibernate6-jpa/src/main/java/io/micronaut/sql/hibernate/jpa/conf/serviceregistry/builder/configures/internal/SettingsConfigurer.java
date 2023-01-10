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
package io.micronaut.sql.hibernate.jpa.conf.serviceregistry.builder.configures.internal;

import io.micronaut.sql.hibernate.jpa.JpaConfiguration;
import io.micronaut.sql.hibernate.jpa.conf.serviceregistry.builder.configures.StandardServiceRegistryBuilderConfigurer;
import io.micronaut.sql.hibernate.jpa.conf.settings.SettingsSupplier;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.util.CollectionUtils;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Prototype
final class SettingsConfigurer implements StandardServiceRegistryBuilderConfigurer {

    private final List<SettingsSupplier> settingsSuppliers;

    SettingsConfigurer(List<SettingsSupplier> settingsSuppliers) {
        this.settingsSuppliers = settingsSuppliers;
    }

    @Override
    public void configure(JpaConfiguration jpaConfiguration, StandardServiceRegistryBuilder standardServiceRegistryBuilder) {
        Map<String, Object> settings = new LinkedHashMap<>(jpaConfiguration.getProperties());
        settings.put(AvailableSettings.SESSION_FACTORY_NAME, jpaConfiguration.getName());
        settings.put(AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false);
        for (SettingsSupplier settingsSupplier : settingsSuppliers) {
            settings.putAll(settingsSupplier.supply(jpaConfiguration));
        }
        if (CollectionUtils.isNotEmpty(settings)) {
            standardServiceRegistryBuilder.applySettings(settings);
        }
    }
}
