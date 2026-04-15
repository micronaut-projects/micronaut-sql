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

import io.micronaut.context.annotation.ConfigurationProperties;
import io.vertx.core.net.PemTrustOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for PostgreSQL PEM trust options.
 * <p>
 * This configuration maps the {@code vertx.pg.client.pem-trust-options} namespace and allows
 * specifying certificate files that should be trusted for SSL connections.
 *
 * @since 6.7.0
 */
@ConfigurationProperties(PgClientSettings.PREFIX + ".pem-trust-options")
final class PgPemTrustOptionsConfiguration {

    /**
     * The certificate paths to trust.
     */
    private List<String> certPaths = new ArrayList<>();

    /**
     * @return The PEM certificate paths configured under
     * {@code vertx.pg.client.pem-trust-options.cert-paths}.
     */
    List<String> getCertPaths() {
        return certPaths;
    }

    /**
     * Sets the PEM certificate paths configured under
     * {@code vertx.pg.client.pem-trust-options.cert-paths}.
     *
     * @param certPaths The certificate paths to trust
     */
    void setCertPaths(List<String> certPaths) {
        this.certPaths = certPaths == null ? new ArrayList<>() : new ArrayList<>(certPaths);
    }

    boolean hasCertPaths() {
        return !certPaths.isEmpty();
    }

    PemTrustOptions asOptions() {
        PemTrustOptions pemTrustOptions = new PemTrustOptions();
        certPaths.forEach(pemTrustOptions::addCertPath);
        return pemTrustOptions;
    }
}
