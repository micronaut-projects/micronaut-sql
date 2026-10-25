package example;

import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class OracleClientNativeTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void queriesThroughTheVertxOracleClient() {
        assertEquals("created", client.toBlocking().retrieve(HttpRequest.POST("/books/setup", "")));
        assertEquals(List.of("The Stand (1978)", "Dune (1965)"),
            client.toBlocking().retrieve(HttpRequest.GET("/books"), Argument.listOf(String.class)));

        Map<String, Object> health = client.toBlocking().retrieve(HttpRequest.GET("/health"), Argument.mapOf(String.class, Object.class));
        assertEquals("UP", health.get("status"));
        assertEquals("UP", ((Map<?, ?>) ((Map<?, ?>) health.get("details")).get("vertx-oracle-client")).get("status"));
    }
}
