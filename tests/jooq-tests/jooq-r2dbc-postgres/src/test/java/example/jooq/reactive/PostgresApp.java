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
package example.jooq.reactive;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest(transactional = false)
@Property(name = "r2dbc.datasources.default.db-type", value = "postgres")
@Property(name = "jooq.r2dbc-datasources.default.sql-dialect", value = "postgres")
@Property(name = "r2dbc.datasources.default.options.driver", value = "pool")
@Property(name = "r2dbc.datasources.default.options.protocol", value = "postgresql")
@Property(name = "r2dbc.datasources.default.options.connectTimeout", value = "PT1M")
@Property(name = "r2dbc.datasources.default.options.statementTimeout", value = "PT1M")
@Property(name = "r2dbc.datasources.default.options.lockTimeout", value = "PT1M")
@Property(name = "test-resources.containers.postgres.image-name", value = "postgres:10")
public class PostgresApp extends AbstractApp {
}
