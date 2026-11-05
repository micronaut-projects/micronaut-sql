package io.micronaut.docs.vertx.oracle

import io.micronaut.context.ApplicationContext
import io.vertx.sqlclient.Pool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.testcontainers.oracle.OracleContainer

class BookCatalogTest {

    @Test
    fun findsATitleThroughTheVertxOracleClient() {
        OracleContainer("gvenzl/oracle-free:23.26.1-faststart").use { oracle ->
            oracle.start()
            ApplicationContext.run(mapOf(
                "vertx.oracle.client.host" to oracle.host,
                "vertx.oracle.client.port" to oracle.oraclePort,
                "vertx.oracle.client.service-name" to oracle.databaseName,
                "vertx.oracle.client.user" to oracle.username,
                "vertx.oracle.client.password" to oracle.password
            )).use { context ->
                val pool = context.getBean(Pool::class.java)
                pool.query("CREATE TABLE book (id NUMBER PRIMARY KEY, title VARCHAR2(100))").execute()
                    .compose { pool.query("INSERT INTO book VALUES (1, 'The Stand')").execute() }
                    .toCompletionStage().toCompletableFuture().get()

                assertEquals("The Stand", context.getBean(BookCatalog::class.java).findTitle(1).toCompletableFuture().get())
            }
        }
    }
}
