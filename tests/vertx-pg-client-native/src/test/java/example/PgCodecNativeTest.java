package example;

import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class PgCodecNativeTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void canInitializeVertxPgCodec() {
        assertEquals("ok", client.toBlocking().retrieve("/postgres/codec"));
    }
}
