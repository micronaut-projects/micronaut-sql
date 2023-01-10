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
package io.micronaut.sql.hibernate.jpa.conf.settings.internal;

import io.micronaut.sql.hibernate.jpa.JpaConfiguration;
import io.micronaut.sql.hibernate.jpa.conf.settings.SettingsSupplier;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Any;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.jdbc.DataSourceResolver;
import org.hibernate.cfg.AvailableSettings;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Map;

/**
 * Data source setting supplier.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Requires(classes = {DataSource.class, DataSourceResolver.class})
@Prototype
final class DataSourceSettingSupplier implements SettingsSupplier {

    @Any
    private final BeanProvider<DataSource> dataSourceBeanProvider;
    private final DataSourceResolver dataSourceResolver;

    DataSourceSettingSupplier(BeanProvider<DataSource> dataSourceBeanProvider, @Nullable DataSourceResolver dataSourceResolver) {
        this.dataSourceBeanProvider = dataSourceBeanProvider;
        this.dataSourceResolver = dataSourceResolver;
    }

    @Override
    public Map<String, Object> supply(JpaConfiguration jpaConfiguration) {
        DataSource dataSource = dataSourceBeanProvider.find(Qualifiers.byName(jpaConfiguration.getName())).orElse(null);
        if (dataSource == null) {
            return Collections.emptyMap();
        }
        if (dataSourceResolver != null) {
            dataSource = dataSourceResolver.resolve(dataSource);
        }
        return Collections.singletonMap(AvailableSettings.DATASOURCE, dataSource);
    }
}
