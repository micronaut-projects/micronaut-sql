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
import io.micronaut.context.annotation.Requires;
import org.jspecify.annotations.Nullable;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.spi.PgDriver;
import io.vertx.sqlclient.Pool;
import jakarta.inject.Singleton;

import io.vertx.core.Future;

/**
 * The Factory for creating Vert.x Pg client.
 */
@Factory
public class PgClientFactory {
    private final PgClientConfiguration connectionConfiguration;
    private final PgPemTrustOptionsConfiguration pemTrustOptionsConfiguration;

    /**
     * The Vert.x instance if you are running with Vert.x.
     */
    private final Vertx vertx;

    /**
     * Create the factory with given Pg Client configuration.
     *
     * @param connectionConfiguration The Pg client configuration
     * @param vertx                   The Vert.x instance
     * @param pemTrustOptionsConfiguration The PEM trust options configuration
     */
    public PgClientFactory(PgClientConfiguration connectionConfiguration,
                           @Nullable Vertx vertx,
                           @Nullable PgPemTrustOptionsConfiguration pemTrustOptionsConfiguration) {
        this.connectionConfiguration = connectionConfiguration;
        this.vertx = vertx;
        this.pemTrustOptionsConfiguration = pemTrustOptionsConfiguration;
    }

    /**
     * @return client A pool of connections.
     */
    @Singleton
    @Requires(missingBeans = Pool.class)
    @Bean(preDestroy = "close")
    public Pool client() {
        return createClient();
    }

    /**
     * Create a connection pool to the database configured with the
     * {@link PgClientConfiguration}.
     *
     * @return A pool of connections.
     */
    private Pool createClient() {
        PgClientConfiguration configuration = this.connectionConfiguration;
        Vertx v = this.vertx != null ? this.vertx : Vertx.vertx();
        PgConnectOptions options = PgConnectOptionsResolver.resolve(configuration, pemTrustOptionsConfiguration);
        return PgDriver.INSTANCE.createPool(v, () -> Future.succeededFuture(options), configuration.poolOptions, configuration.netClientOptions, null);
    }
}
