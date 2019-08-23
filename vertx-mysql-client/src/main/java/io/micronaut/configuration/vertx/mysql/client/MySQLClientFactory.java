/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.micronaut.core.util.StringUtils;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.mysqlclient.MySQLPool;

import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 * The Factory for creating Vertx MySQL client.
 *
 */
@Factory
public class MySQLClientFactory {
    private final MySQLConnectionConfiguration connectionConfiguration;

    private final MySQLPoolConfiguration poolConfiguration;

    /**
     * The Vertx instance if you are running with Vert.x.
     */
    private final Vertx vertx;

    /**
     * Create the factory with given MySQL Client configuration and
     *
     * @param connectionConfiguration The  MySQL ClientOption configurations
     * @param poolConfiguration The MySQL Pool configurations
     */
    public MySQLClientFactory(MySQLConnectionConfiguration connectionConfiguration,
                              MySQLPoolConfiguration poolConfiguration, @Nullable Vertx vertx) {
        this.connectionConfiguration = connectionConfiguration;
        this.poolConfiguration = poolConfiguration;
        this.vertx = vertx;
    }

    /**
     * @return client A pool of connections.
     */
    @Singleton
    @Bean(preDestroy = "close")
    public MySQLPool client() {
        if (this.vertx == null) {
            return createClient();
        } else {
            return createClient(vertx);
        }
    }

    /**
     * Create a connection pool to the database configured with the
     * {@link MySQLConnectionConfiguration}.{@link MySQLPoolConfiguration}
     * @return A pool of connections.
     */
    private MySQLPool createClient() {
        MySQLConnectionConfiguration configuration = this.connectionConfiguration;
        MySQLPoolConfiguration poolConfiguration = this.poolConfiguration;
        String connectionUri = configuration.getUri();
        if (StringUtils.isNotEmpty(connectionUri)) {
            return MySQLPool.pool(connectionUri);
        } else {
            return MySQLPool.pool(configuration.connectOptions,poolConfiguration.poolOptions);
        }
    }

    /**
     * Create a connection pool to the database configured with the {@link MySQLConnectionConfiguration },{@link MySQLPoolConfiguration}.
     * @param vertx The Vertx instance.
     * @return A pool of connections.
     */
    private MySQLPool createClient(Vertx vertx) {
        MySQLConnectionConfiguration configuration = this.connectionConfiguration;
        MySQLPoolConfiguration poolConfiguration = this.poolConfiguration;
        String connectionUri = configuration.getUri();
        if (StringUtils.isNotEmpty(connectionUri)) {
            return MySQLPool.pool(vertx,connectionUri);
        } else {
            return MySQLPool.pool(vertx,configuration.connectOptions,poolConfiguration.poolOptions);
        }
    }
}
