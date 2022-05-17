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
package io.micronaut.configuration.vertx.pg.client;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.util.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.spi.PgDriver;
import io.vertx.sqlclient.Pool;
import jakarta.inject.Singleton;

import java.util.Collections;

/**
 * The Factory for creating Vertx Pg client.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Factory
class PgDriverFactory {
    private final PgClientConfiguration connectionConfiguration;

    /**
     * The Vertx instance if you are running with Vert.x.
     */
    private final Vertx vertx;

    /**
     * Create the factory with given Pg Client configuration.
     *
     * @param connectionConfiguration The  Pg ClientOption configurations
     * @param vertx                   The vertx instance
     */
    PgDriverFactory(PgClientConfiguration connectionConfiguration, @Nullable Vertx vertx) {
        this.connectionConfiguration = connectionConfiguration;
        this.vertx = vertx;
    }

    /**
     * @return client A pool of connections.
     */
    @Singleton
    @Bean(preDestroy = "close")
    Pool build() {
        String connectionUri = connectionConfiguration.getUri();
        if (StringUtils.isNotEmpty(connectionUri)) {
            PgConnectOptions pgConnectOptions = PgDriver.INSTANCE.parseConnectionUri(connectionUri);
            return PgDriver.INSTANCE.createPool(vertx, Collections.singletonList(pgConnectOptions), connectionConfiguration.poolOptions);
        }
        return PgDriver.INSTANCE.createPool(vertx, Collections.singletonList(connectionConfiguration.connectOptions), connectionConfiguration.poolOptions);
    }
}
