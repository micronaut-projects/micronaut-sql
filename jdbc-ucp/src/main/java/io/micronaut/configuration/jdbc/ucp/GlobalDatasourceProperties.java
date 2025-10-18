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
package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.convert.format.MapFormat;
import io.micronaut.core.naming.conventions.StringConvention;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for global datasource settings that can be applied
 * to all {@link DatasourceConfiguration} instances. This class binds configuration
 * properties under the "global.datasources" prefix and provides a way to define
 * common datasource properties that should be applied across all datasources
 * in the application.
 *
 * <p>Properties defined here serve as defaults that can be overridden by
 * individual datasource configurations. The primary use case is to avoid
 * repetition when multiple datasources share common settings such as
 * connection pool parameters, SSL settings, or application-specific properties.</p>
 *
 * <p>This bean is only created when the "global.datasources.data-source-properties"
 * configuration property is present, ensuring it doesn't interfere with applications
 * that don't use global datasource configuration.</p>
 *
 * @author James Forward
 */
@Requires(property = "global.datasources.data-source-properties")
@ConfigurationProperties("global.datasources")
public class GlobalDatasourceProperties {
    private Map<String, Object> dataSourceProperties = new HashMap<>();

    public Map<String, Object> getDataSourceProperties() {
        return dataSourceProperties;
    }

    public void setDataSourceProperties(@MapFormat(transformation = MapFormat.MapTransformation.FLAT, keyFormat = StringConvention.RAW) Map<String, ?> dataSourceProperties) {
        this.dataSourceProperties.putAll(dataSourceProperties);
    }
}
