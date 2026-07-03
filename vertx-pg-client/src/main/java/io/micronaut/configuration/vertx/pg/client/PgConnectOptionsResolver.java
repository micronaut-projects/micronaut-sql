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
package io.micronaut.configuration.vertx.pg.client;

import io.micronaut.core.util.StringUtils;
import io.vertx.core.net.ClientSSLOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.spi.PgDriver;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Internal helper that resolves the effective {@link PgConnectOptions} for pool creation,
 * supporting both URI-based and property-based configuration and applying any
 * PEM trust certificates that cannot be expressed via the normal property-binding path.
 */
final class PgConnectOptionsResolver {

    private PgConnectOptionsResolver() {
    }

    /**
     * Resolves effective connect options from the given configuration, applying PEM trust
     * certificates when present.
     *
     * @param configuration                the client configuration
     * @param pemTrustOptionsConfiguration optional PEM trust certificate configuration
     * @return the effective connect options ready for pool creation
     */
    static PgConnectOptions resolve(PgClientConfiguration configuration, @Nullable PgPemTrustOptionsConfiguration pemTrustOptionsConfiguration) {
        String connectionUri = configuration.getUri();
        PgConnectOptions configuredOptions = configuration.getConnectOptions();
        PgConnectOptions connectOptions;
        if (StringUtils.isNotEmpty(connectionUri)) {
            connectOptions = Objects.requireNonNull(PgDriver.INSTANCE.parseConnectionUri(connectionUri));
            applyConfiguredSslOptions(connectOptions, configuredOptions);
        } else {
            connectOptions = new PgConnectOptions(configuredOptions);
        }
        applyPemTrustOptions(connectOptions, pemTrustOptionsConfiguration);
        return connectOptions;
    }

    private static void applyConfiguredSslOptions(PgConnectOptions connectOptions, PgConnectOptions configuredOptions) {
        if (configuredOptions.getSslMode() != PgConnectOptions.DEFAULT_SSLMODE) {
            connectOptions.setSslMode(configuredOptions.getSslMode());
        }
        ClientSSLOptions configuredSslOptions = configuredOptions.getSslOptions();
        String configuredHostnameVerificationAlgorithm = configuredSslOptions == null ? null : configuredSslOptions.getHostnameVerificationAlgorithm();
        String defaultHostnameVerificationAlgorithm = new ClientSSLOptions().getHostnameVerificationAlgorithm();
        if (!Objects.equals(configuredHostnameVerificationAlgorithm, defaultHostnameVerificationAlgorithm)) {
            ClientSSLOptions sslOptions = connectOptions.getSslOptions();
            if (sslOptions == null) {
                sslOptions = new ClientSSLOptions();
                connectOptions.setSslOptions(sslOptions);
            }
            sslOptions.setHostnameVerificationAlgorithm(configuredHostnameVerificationAlgorithm);
        }
    }

    private static void applyPemTrustOptions(PgConnectOptions connectOptions, @Nullable PgPemTrustOptionsConfiguration pemTrustOptionsConfiguration) {
        if (pemTrustOptionsConfiguration == null || !pemTrustOptionsConfiguration.hasCertPaths()) {
            return;
        }
        ClientSSLOptions sslOptions = connectOptions.getSslOptions();
        if (sslOptions == null) {
            sslOptions = new ClientSSLOptions();
            connectOptions.setSslOptions(sslOptions);
        }
        if (sslOptions.getTrustOptions() == null) {
            sslOptions.setTrustOptions(pemTrustOptionsConfiguration.asOptions());
        }
    }
}
