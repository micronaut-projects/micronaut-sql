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

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.oracleclient.spi.OracleDriver;
import io.vertx.sqlclient.Pool;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * The factory for the Vert.x Oracle client {@link Pool}.
 *
 * @since 7.3.0
 */
@Factory
public class OracleClientFactory {

    private final OracleClientConfiguration configuration;

    /**
     * The Vert.x instance if you are running with Vert.x.
     */
    private final @Nullable Vertx vertx;

    /**
     * @param configuration The Oracle client configuration
     * @param vertx         The Vert.x instance, or {@code null} to create one
     */
    public OracleClientFactory(OracleClientConfiguration configuration, @Nullable Vertx vertx) {
        this.configuration = configuration;
        this.vertx = vertx;
    }

    /**
     * @return A pool of connections to the database configured with the {@link OracleClientConfiguration}.
     */
    @Singleton
    @Requires(missingBeans = Pool.class)
    @Bean(preDestroy = "close")
    public Pool client() {
        String uri = configuration.getUri();
        OracleConnectOptions options = StringUtils.isNotEmpty(uri)
            ? fromUri(uri, configuration.connectOptions)
            : configuration.connectOptions;
        Vertx v = vertx != null ? vertx : Vertx.vertx();
        return OracleDriver.INSTANCE.createPool(v, () -> Future.succeededFuture(options),
            configuration.poolOptions, configuration.netClientOptions, null);
    }

    private static OracleConnectOptions fromUri(String uri, OracleConnectOptions configured) {
        OracleConnectOptions options = OracleDriver.INSTANCE.parseConnectionUri(uri);
        // Oracle URIs rarely carry credentials, so the configured user and password apply when the URI has none
        if (StringUtils.isEmpty(options.getUser())) {
            options.setUser(configured.getUser());
        }
        if (StringUtils.isEmpty(options.getPassword())) {
            options.setPassword(configured.getPassword());
        }
        return options;
    }
}
