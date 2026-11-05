/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.configuration.vertx.oracle.client;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import org.jspecify.annotations.Nullable;

/**
 * The configuration of the Vert.x Oracle client: the {@link OracleConnectOptions} and {@link PoolOptions}, and
 * optionally a connection URI. The Oracle client connects through JDBC, so it has no Vert.x net client options.
 *
 * @since 7.3.0
 */
@ConfigurationProperties(OracleClientSettings.PREFIX)
public class OracleClientConfiguration {

    @ConfigurationBuilder
    protected OracleConnectOptions connectOptions = new OracleConnectOptions();

    @ConfigurationBuilder
    protected PoolOptions poolOptions = new PoolOptions();

    protected @Nullable String uri;

    /**
     * @return The Oracle connection URI, for example {@code oracle:thin:@localhost:1521/FREEPDB1} or
     * {@code jdbc:oracle:thin:@mydb_high}. It replaces the configured endpoint, and the other configured options
     * still apply.
     */
    public @Nullable String getUri() {
        return uri;
    }

    /**
     * @return The options for configuring a connection.
     */
    public OracleConnectOptions getConnectOptions() {
        return connectOptions;
    }

    /**
     * @return The options for configuring a connection pool.
     */
    public PoolOptions getPoolOptions() {
        return poolOptions;
    }
}
