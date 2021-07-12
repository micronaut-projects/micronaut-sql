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
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.pgclient.PgPool;
import jakarta.inject.Singleton;

/**
 * The Factory for creating Vertx Pg client.
 *
 */
@Factory
public class PgClientFactory {
    private final PgClientConfiguration connectionConfiguration;

    /**
     * The Vertx instance if you are running with Vert.x.
     */
    private final Vertx vertx;

    /**
     * Create the factory with given Pg Client configuration.
     *
     * @param connectionConfiguration The  Pg ClientOption configurations
     * @param vertx  The vertx instance
     */
    public PgClientFactory(PgClientConfiguration connectionConfiguration, @Nullable Vertx vertx) {
        this.connectionConfiguration = connectionConfiguration;
        this.vertx = vertx;
    }

    /**
     * @return client A pool of connections.
     */
    @Singleton
    @Bean(preDestroy = "close")
    public PgPool client() {
        if (this.vertx == null) {
            return createClient();
        } else {
            return createClient(vertx);
        }
    }

    /**
     * Create a connection pool to the database configured with the
     * {@link PgClientConfiguration}.
     * @return A pool of connections.
     */
    private PgPool createClient() {
        PgClientConfiguration configuration = this.connectionConfiguration;
        String connectionUri = configuration.getUri();
        if (StringUtils.isNotEmpty(connectionUri)) {
            return PgPool.pool(connectionUri, configuration.poolOptions);
        } else {
            return PgPool.pool(configuration.connectOptions, configuration.poolOptions);
        }
    }

    /**
     * Create a connection pool to the database configured with the {@link PgClientConfiguration }.
     * @param vertx The Vertx instance.
     * @return A pool of connections.
     */
    private PgPool createClient(Vertx vertx) {
        PgClientConfiguration configuration = this.connectionConfiguration;
        String connectionUri = configuration.getUri();
        if (StringUtils.isNotEmpty(connectionUri)) {
            return PgPool.pool(vertx, connectionUri, configuration.poolOptions);
        } else {
            return PgPool.pool(vertx, configuration.connectOptions, configuration.poolOptions);
        }
    }
}
