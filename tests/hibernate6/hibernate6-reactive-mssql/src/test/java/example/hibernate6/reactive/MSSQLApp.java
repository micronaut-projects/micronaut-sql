/*
 * Copyright 2017-2023 original authors
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
package example.hibernate6.reactive;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest(transactional = false)
@Property(name = "jpa.default.properties.hibernate.connection.db-type", value = "mssql")
@Property(name = "jpa.default.reactive", value = "true")
public class MSSQLApp extends AbstractApp {

    static {
        // In netty 4.2.2 SslContextBuilder defaults endpointIdentificationAlgorithm to "HTTPS" if system property
        // "io.netty.handler.ssl.defaultEndpointVerificationAlgorithm" is not set. This makes sun.security.util.HostnameChecker.match(String expectedName, X509Certificate cert, boolean chainsToPublicCA)
        // method to throw error "javax.net.ssl.SSLHandshakeException: No name matching localhost found" and this is workaround.
        // Which could be documented or suggested if users experience this issue
        System.setProperty("io.netty.handler.ssl.defaultEndpointVerificationAlgorithm", "NONE");
    }
}
