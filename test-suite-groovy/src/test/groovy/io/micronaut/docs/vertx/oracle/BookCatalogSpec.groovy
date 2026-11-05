package io.micronaut.docs.vertx.oracle

import io.micronaut.context.ApplicationContext
import io.vertx.sqlclient.Pool
import org.testcontainers.oracle.OracleContainer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class BookCatalogSpec extends Specification {

    @Shared @AutoCleanup OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23.26.1-faststart")

    void "test finding a title through the Vert.x Oracle client"() {
        given:
        oracle.start()
        ApplicationContext context = ApplicationContext.run(
            "vertx.oracle.client.host": oracle.host,
            "vertx.oracle.client.port": oracle.oraclePort,
            "vertx.oracle.client.service-name": oracle.databaseName,
            "vertx.oracle.client.user": oracle.username,
            "vertx.oracle.client.password": oracle.password
        )
        Pool pool = context.getBean(Pool)
        pool.query("CREATE TABLE book (id NUMBER PRIMARY KEY, title VARCHAR2(100))").execute()
            .compose { pool.query("INSERT INTO book VALUES (1, 'The Stand')").execute() }
            .toCompletionStage().toCompletableFuture().get()

        expect:
        context.getBean(BookCatalog).findTitle(1).toCompletableFuture().get() == "The Stand"

        cleanup:
        context?.close()
    }
}
