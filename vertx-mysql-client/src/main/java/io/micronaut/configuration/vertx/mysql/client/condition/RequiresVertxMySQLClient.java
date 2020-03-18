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
package io.micronaut.configuration.vertx.mysql.client.condition;

import io.micronaut.configuration.vertx.mysql.client.MySQLClientSettings;
import io.micronaut.context.annotation.Requires;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.PoolOptions;

import java.lang.annotation.*;

/**
 * Custom condition to indicate bean requires the vertx mysql client.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PACKAGE, ElementType.TYPE})
@Requires(property = MySQLClientSettings.PREFIX)
@Requires(classes = {MySQLConnectOptions.class, PoolOptions.class})
public @interface RequiresVertxMySQLClient {
}
