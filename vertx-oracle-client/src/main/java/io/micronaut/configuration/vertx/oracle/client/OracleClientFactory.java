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
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.util.StringUtils;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.NetClientOptions;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.oracleclient.impl.OracleConnectionUriParser;
import io.vertx.oracleclient.spi.OracleDriver;
import io.vertx.sqlclient.Pool;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The factory for the Vert.x Oracle client {@link Pool}.
 *
 * @since 7.3.0
 */
@Factory
public class OracleClientFactory {

    private static final String JDBC_PREFIX = "jdbc:";
    // the options that name the database, all replaced by a URI that names one
    private static final List<String> ENDPOINT_OPTIONS = List.of("host", "port", "serviceId", "serviceName", "tnsAlias");

    private final OracleClientConfiguration configuration;

    /**
     * The Vert.x instance if you are running with Vert.x.
     */
    private final @Nullable Vertx vertx;

    /**
     * The Vert.x instance created when there is no Vert.x bean, closed with the factory.
     */
    private @Nullable Vertx createdVertx;

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
        Vertx v = vertx;
        if (v == null) {
            v = Vertx.vertx();
            createdVertx = v;
        }
        // the Oracle driver connects through JDBC and ignores the net client options
        return OracleDriver.INSTANCE.createPool(v, () -> Future.succeededFuture(options),
            configuration.poolOptions, new NetClientOptions(), null);
    }

    /**
     * Closes the Vert.x instance this factory created, whose threads would otherwise keep the JVM running.
     */
    @PreDestroy
    void close() {
        if (createdVertx != null) {
            createdVertx.close();
            createdVertx = null;
        }
    }

    /**
     * Applies the options a URI sets to a copy of the configured options, so the other configured options, such as
     * the {@code tns-admin} directory of a TNS alias, still apply. A URI that names the database replaces the whole
     * configured endpoint.
     *
     * @param uri        The URI, with or without the {@code jdbc:} prefix
     * @param configured The configured options
     * @return The options to connect with
     */
    static OracleConnectOptions fromUri(String uri, OracleConnectOptions configured) {
        String oracleUri = uri.regionMatches(true, 0, JDBC_PREFIX, 0, JDBC_PREFIX.length()) ? uri.substring(JDBC_PREFIX.length()) : uri;
        JsonObject parsed = OracleConnectionUriParser.parse(oracleUri, false);
        if (parsed == null) {
            throw new ConfigurationException("Invalid Oracle connection URI [" + uri + "], expected oracle:thin:@<database> or jdbc:oracle:thin:@<database>");
        }
        JsonObject options = configured.toJson();
        if (ENDPOINT_OPTIONS.stream().anyMatch(parsed::containsKey)) {
            ENDPOINT_OPTIONS.forEach(options::remove);
        }
        return new OracleConnectOptions(options.mergeIn(parsed));
    }
}
