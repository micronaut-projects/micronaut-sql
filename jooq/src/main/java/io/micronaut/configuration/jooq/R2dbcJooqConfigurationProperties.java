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
package io.micronaut.configuration.jooq;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Requires;
import io.r2dbc.spi.ConnectionFactory;

/**
 * R2DBC configuration for jOOQ.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Requires(classes = ConnectionFactory.class)
@EachProperty(value = "jooq.r2dbc-datasources")
public final class R2dbcJooqConfigurationProperties extends AbstractJooqConfigurationProperties {
}
