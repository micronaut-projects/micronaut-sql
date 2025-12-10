/*
 * Copyright 2017-2025 original authors
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
 * Provides support for unpooled JDBC datasources in Micronaut.
 *
 * <p>This package contains an implementation of DataSource that creates
 * a new physical database connection for each getConnection()
 * call, without using a connection pool.</p>
 *
 * <p><strong>Performance Warning:</strong> Unpooled datasources have significant performance implications.
 * Creating a new connection typically takes 10-100ms, compared to approximately 1ms for retrieving
 * a connection from a pool. This implementation should only be used for:</p>
 *
 * <ul>
 *   <li>Testing scenarios that require clean connection state</li>
 *   <li>Serverless/FaaS applications with short-lived executions</li>
 *   <li>Very low-volume applications (less than 1 request/second)</li>
 *   <li>Educational or prototype applications</li>
 * </ul>
 *
 * <p>For production applications with normal load, use a pooled datasource implementation
 * such as HikariCP, Tomcat JDBC Pool, or Apache DBCP.</p>
 *
 * @since 6.3.0
 */
package io.micronaut.configuration.jdbc.unpooled;
