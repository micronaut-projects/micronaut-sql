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
import io.vertx.core.net.NetClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.spi.PgDriver;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

final class PgConnectOptionsResolver {

    private PgConnectOptionsResolver() {
    }

    static PgConnectOptions resolve(PgClientConfiguration configuration, @Nullable PgPemTrustOptionsConfiguration pemTrustOptionsConfiguration) {
        String connectionUri = configuration.getUri();
        PgConnectOptions connectOptions = StringUtils.isNotEmpty(connectionUri)
            ? Objects.requireNonNull(PgDriver.INSTANCE.parseConnectionUri(connectionUri))
            : new PgConnectOptions(configuration.getConnectOptions());
        applyNetClientOptions(connectOptions, configuration.getNetClientOptions());
        applyPemTrustOptions(connectOptions, pemTrustOptionsConfiguration);
        return connectOptions;
    }

    private static void applyNetClientOptions(PgConnectOptions connectOptions, NetClientOptions netClientOptions) {
        ClientSSLOptions connectSslOptions = connectOptions.getSslOptions();
        ClientSSLOptions netClientSslOptions = netClientOptions.getSslOptions();
        if (netClientSslOptions == null) {
            return;
        }
        if (connectSslOptions == null) {
            connectOptions.setSslOptions(netClientSslOptions.copy());
            return;
        }
        if (connectSslOptions.getTrustOptions() == null && netClientSslOptions.getTrustOptions() != null) {
            connectSslOptions.setTrustOptions(netClientSslOptions.getTrustOptions());
        }
        if (connectSslOptions.getKeyCertOptions() == null && netClientSslOptions.getKeyCertOptions() != null) {
            connectSslOptions.setKeyCertOptions(netClientSslOptions.getKeyCertOptions());
        }
        if (!connectSslOptions.isTrustAll() && netClientSslOptions.isTrustAll()) {
            connectSslOptions.setTrustAll(true);
        }
        if (StringUtils.isEmpty(connectSslOptions.getHostnameVerificationAlgorithm())
            && StringUtils.isNotEmpty(netClientSslOptions.getHostnameVerificationAlgorithm())) {
            connectSslOptions.setHostnameVerificationAlgorithm(netClientSslOptions.getHostnameVerificationAlgorithm());
        }
        if (!connectSslOptions.isUseAlpn() && netClientSslOptions.isUseAlpn()) {
            connectSslOptions.setUseAlpn(true);
        }
        if (connectSslOptions.getEnabledCipherSuites().isEmpty() && !netClientSslOptions.getEnabledCipherSuites().isEmpty()) {
            netClientSslOptions.getEnabledCipherSuites().forEach(connectSslOptions::addEnabledCipherSuite);
        }
        if (connectSslOptions.getEnabledSecureTransportProtocols().isEmpty() && !netClientSslOptions.getEnabledSecureTransportProtocols().isEmpty()) {
            netClientSslOptions.getEnabledSecureTransportProtocols().forEach(connectSslOptions::addEnabledSecureTransportProtocol);
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
