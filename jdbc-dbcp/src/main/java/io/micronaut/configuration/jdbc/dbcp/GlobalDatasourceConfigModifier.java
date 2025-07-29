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

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * A bean created event listener that applies global datasource properties to
 * {@link DatasourceConfiguration} beans when they are created. This modifier
 * ensures that global properties defined under the "global.datasources.data-source-properties"
 * configuration prefix are automatically applied to all datasource configurations,
 * while preserving individual datasource-specific settings that take precedence.
 *
 * <p>The modifier only adds global properties that are not already present in the
 * individual datasource configuration, ensuring that specific configurations always
 * override global defaults. Properties with null values are ignored.</p>
 *
 * <p>This bean is only created when the "global.datasources.data-source-properties"
 * configuration property is present.</p>
 *
 * @author James Forward
 */
@Requires(property = "global.datasources.data-source-properties")
@Singleton
public class GlobalDatasourceConfigModifier implements BeanCreatedEventListener<DatasourceConfiguration> {

    @Inject
    GlobalDatasourceProperties globalDatasourceProperties;

    @Override
    public DatasourceConfiguration onCreated(BeanCreatedEvent<DatasourceConfiguration> event) {

        DatasourceConfiguration configuration = event.getBean();
        globalDatasourceProperties.getDataSourceProperties()
            .forEach((key, value) -> {
                    if (value != null && !configuration.getIndividualDsProperties().containsKey(key)) {
                        configuration.addConnectionProperty(key, value);
                    }
                }
            );
        return configuration;
    }
}
