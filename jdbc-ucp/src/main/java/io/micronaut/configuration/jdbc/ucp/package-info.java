/*
 * Copyright 2017-2024 original authors
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
/**
 * Configuration for Tomcat JDBC data sources.
 */
@Configuration
@Requires(classes = PoolDataSource.class)
@Requires(property = BasicJdbcConfiguration.PREFIX + ".enabled", value = StringUtils.TRUE, defaultValue = StringUtils.TRUE)
package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.context.annotation.Configuration;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.jdbc.BasicJdbcConfiguration;
import oracle.ucp.jdbc.PoolDataSource;
