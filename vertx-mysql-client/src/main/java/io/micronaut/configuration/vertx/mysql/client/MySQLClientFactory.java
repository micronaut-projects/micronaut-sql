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
package io.micronaut.configuration.vertx.mysql.client;

import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import org.jspecify.annotations.Nullable;
import io.micronaut.core.util.StringUtils;
import io.vertx.core.Vertx;
import io.vertx.core.Future;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.spi.MySQLDriver;
import io.vertx.sqlclient.Pool;
import jakarta.inject.Singleton;

/**
 * The Factory for creating Vertx MySQL client.
 *
 */
@Factory
public class MySQLClientFactory {
    private final MySQLClientConfiguration connectionConfiguration;

    /**
     * The Vertx instance if you are running with Vert.x.
     */
    private final @Nullable Vertx vertx;

    /**
     * Create the factory with given MySQL Client configuration.
     *
     * @param connectionConfiguration The  MySQL ClientOption configurations
     * @param vertx The vertx instance
     */
    public MySQLClientFactory(MySQLClientConfiguration connectionConfiguration, @Nullable Vertx vertx) {
        this.connectionConfiguration = connectionConfiguration;
        this.vertx = vertx;
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
     * {@link MySQLClientConfiguration}.
     * @return A pool of connections.
     */
    private Pool createClient() {
        MySQLClientConfiguration configuration = this.connectionConfiguration;
        String connectionUri = configuration.getUri();
        Vertx v = this.vertx != null ? this.vertx : Vertx.vertx();
        if (StringUtils.isNotEmpty(connectionUri)) {
            MySQLConnectOptions options = MySQLDriver.INSTANCE.parseConnectionUri(connectionUri);
            return MySQLDriver.INSTANCE.createPool(v, () -> Future.succeededFuture(options), configuration.poolOptions, configuration.netClientOptions, null);
        } else {
            return MySQLDriver.INSTANCE.createPool(v, () -> Future.succeededFuture(configuration.connectOptions), configuration.poolOptions, configuration.netClientOptions, null);
        }
    }

}
