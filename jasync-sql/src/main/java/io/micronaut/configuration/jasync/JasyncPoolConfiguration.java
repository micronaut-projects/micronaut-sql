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

import com.github.jasync.sql.db.ConnectionPoolConfiguration;
import com.github.jasync.sql.db.ConnectionPoolConfigurationBuilder;
import com.github.jasync.sql.db.SSLConfiguration;
import com.github.jasync.sql.db.interceptor.QueryInterceptor;
import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationInject;
import io.micronaut.context.annotation.ConfigurationProperties;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.bind.annotation.Bindable;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

/**
 * The configuration class for Jasync Client.
 *
 * @author oshai
 * @since 1.0
 */
@ConfigurationProperties(JasyncClientSettings.PREFIX)
public class JasyncPoolConfiguration {

    @ConfigurationBuilder
    protected ConnectionPoolConfigurationBuilder jasyncOptions = new ConnectionPoolConfigurationBuilder();

    /**
     * Injected constructor.
     * @param sslConfiguration The SSL config
     * @param queryInterceptors Query Interceptors
     */
    protected JasyncPoolConfiguration(@Nullable JasyncSslConfiguration sslConfiguration,
                                      @Nullable List<QueryInterceptor> queryInterceptors) {
        if (sslConfiguration != null) {
            jasyncOptions.setSsl(sslConfiguration.getSslConfiguration());
        }

        if (queryInterceptors != null) {
            jasyncOptions.setInterceptors(queryInterceptors
                    .stream()
                    .map(x -> (Supplier<QueryInterceptor>) () -> x)
                    .toList()
            );
        }
    }

    /**
     *
     * @return The options for configuring a connection pool.
     */
    public ConnectionPoolConfiguration getJasyncOptions() {
        return jasyncOptions.build();
    }

    /**
     * Configuration for JAsync SSL.
     */
    @ConfigurationProperties("ssl")
    @Requires(property = JasyncClientSettings.PREFIX + ".ssl")
    public static class JasyncSslConfiguration {

        private final SSLConfiguration sslConfiguration;

        /**
         * Default constructor.
         * @param mode The mode
         * @param rootCert The cert
         * @param clientCert The client cert
         * @param clientPrivateKey The client private key
         */
        @ConfigurationInject
        public JasyncSslConfiguration(
                @Bindable(defaultValue = "Disable")
                SSLConfiguration.Mode mode,
                @Nullable String rootCert,
                @Nullable String clientCert,
                @Nullable String clientPrivateKey) {
            this.sslConfiguration = new SSLConfiguration(
                    mode,
                    rootCert != null ? new File(rootCert) : null,
                    clientCert != null ? new File(clientCert) : null,
                    clientPrivateKey != null ? new File(clientPrivateKey) : null
            );
        }

        /**
         * @return The SSL configuration
         */
        @NonNull
        public SSLConfiguration getSslConfiguration() {
            return sslConfiguration;
        }
    }
}
