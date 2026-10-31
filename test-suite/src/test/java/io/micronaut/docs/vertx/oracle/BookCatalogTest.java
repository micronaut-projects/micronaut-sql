package io.micronaut.docs.vertx.oracle;

import io.micronaut.context.ApplicationContext;
import io.vertx.sqlclient.Pool;
import org.junit.jupiter.api.Test;
import org.testcontainers.oracle.OracleContainer;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BookCatalogTest {

    @Test
    void findsATitleThroughTheVertxOracleClient() throws Exception {
        try (OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23.26.1-faststart")) {
            oracle.start();
            try (ApplicationContext context = ApplicationContext.run(Map.of(
                "vertx.oracle.client.host", oracle.getHost(),
                "vertx.oracle.client.port", oracle.getOraclePort(),
                "vertx.oracle.client.service-name", oracle.getDatabaseName(),
                "vertx.oracle.client.user", oracle.getUsername(),
                "vertx.oracle.client.password", oracle.getPassword()))) {
                Pool pool = context.getBean(Pool.class);
                pool.query("CREATE TABLE book (id NUMBER PRIMARY KEY, title VARCHAR2(100))").execute()
                    .compose(ignored -> pool.query("INSERT INTO book VALUES (1, 'The Stand')").execute())
                    .toCompletionStage().toCompletableFuture().get();

                assertEquals("The Stand", context.getBean(BookCatalog.class).findTitle(1).toCompletableFuture().get());
            }
        }
    }
}
