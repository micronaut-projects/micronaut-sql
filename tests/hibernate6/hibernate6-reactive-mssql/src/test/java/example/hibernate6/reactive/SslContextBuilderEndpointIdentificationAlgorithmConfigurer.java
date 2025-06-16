package example.hibernate6.reactive;

import io.micronaut.context.annotation.Context;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;

/**
 * In netty 4.2.2 SslContextBuilder defaults endpointIdentificationAlgorithm to "HTTPS" if system property
 * "io.netty.handler.ssl.defaultEndpointVerificationAlgorithm" is not set. This makes sun.security.util.HostnameChecker.match(String expectedName, X509Certificate cert, boolean chainsToPublicCA)
 * method to throw error "javax.net.ssl.SSLHandshakeException: No name matching localhost found" and this is workaround.
 */
@Context
@Singleton
class SslContextBuilderEndpointIdentificationAlgorithmConfigurer {

    @PostConstruct
    void init() {
        System.setProperty("io.netty.handler.ssl.defaultEndpointVerificationAlgorithm", "NONE");
    }
}
