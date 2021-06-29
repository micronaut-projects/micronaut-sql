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
package io.micronaut.configuration.jasync;

import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection;
import com.github.jasync.sql.db.postgresql.PostgreSQLConnectionBuilder;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

import javax.annotation.PreDestroy;

/**
 * The Factory for creating Reactive Postgres client.
 */
@Factory
@Requires(classes = PostgreSQLConnectionBuilder.class)
public class JasyncPostgreSQLClientFactory implements AutoCloseable {

    private final JasyncPoolConfiguration jasyncPoolConfiguration;
    private ConnectionPool<PostgreSQLConnection> connection;

    /**
     * Create the factory with given Pool configuration.
     *
     * @param jasyncPoolConfiguration The Reactive Postgres configurations
     */
    public JasyncPostgreSQLClientFactory(JasyncPoolConfiguration jasyncPoolConfiguration) {
        this.jasyncPoolConfiguration = jasyncPoolConfiguration;
    }

    /**
     * Create a connection pool to the database configured with the {@link JasyncPoolConfiguration}.
     * @return client A pool of connections.
     */
    @Singleton
    public Connection client() {
        if (this.connection == null || !this.connection.isConnected()) {
            this.connection = PostgreSQLConnectionBuilder.createConnectionPool(this.jasyncPoolConfiguration.jasyncOptions);
        }
        return this.connection;
    }

    @Override
    @PreDestroy
    public void close() {
        if (this.connection != null && this.connection.isConnected()) {
            this.connection.disconnect();
        }
    }
}
