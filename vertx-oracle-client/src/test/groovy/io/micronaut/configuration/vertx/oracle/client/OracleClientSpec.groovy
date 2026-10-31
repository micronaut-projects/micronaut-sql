package io.micronaut.configuration.vertx.oracle.client

import io.micronaut.context.ApplicationContext
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.Tuple
import org.testcontainers.oracle.OracleContainer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class OracleClientSpec extends Specification {

    static final String IMAGE_NAME = "gvenzl/oracle-free:23.26.1-faststart"

    @Shared @AutoCleanup OracleContainer oracle = new OracleContainer(IMAGE_NAME)

    def setupSpec() {
        oracle.start()
    }

    void "test querying through the Vert.x Oracle client pool"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.host': oracle.host,
                'vertx.oracle.client.port': oracle.oraclePort,
                'vertx.oracle.client.service-name': oracle.databaseName,
                'vertx.oracle.client.user': oracle.username,
                'vertx.oracle.client.password': oracle.password,
                'vertx.oracle.client.max-size': '2'
        )
        Pool pool = applicationContext.getBean(Pool)

        when:
        pool.query("CREATE TABLE book (id NUMBER PRIMARY KEY, title VARCHAR2(100))").execute()
            .compose { pool.preparedQuery("INSERT INTO book (id, title) VALUES (?, ?)").executeBatch([Tuple.of(1, "The Stand"), Tuple.of(2, "Dune")]) }
            .toCompletionStage().toCompletableFuture().get()
        List<String> titles = pool.preparedQuery("SELECT title FROM book WHERE id >= ? ORDER BY id").execute(Tuple.of(1))
            .map { rows -> rows.collect { Row row -> row.getString("TITLE") } }
            .toCompletionStage().toCompletableFuture().get()

        then:
        titles == ["The Stand", "Dune"]

        cleanup:
        pool?.query("DROP TABLE book")?.execute()?.toCompletionStage()?.toCompletableFuture()?.get()
        applicationContext?.close()
    }

    void "test connecting with a connection URI"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.uri': "oracle:thin:@${oracle.host}:${oracle.oraclePort}/${oracle.databaseName}",
                'vertx.oracle.client.user': oracle.username,
                'vertx.oracle.client.password': oracle.password
        )

        when:
        String result = applicationContext.getBean(Pool)
            .query("SELECT 'connected' FROM dual").execute()
            .map { rows -> rows.iterator().next().getString(0) }
            .toCompletionStage().toCompletableFuture().get()

        then:
        result == "connected"

        cleanup:
        applicationContext?.close()
    }

    void "test the configured options apply with a JDBC URL"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.uri': "jdbc:oracle:thin:@${oracle.host}:${oracle.oraclePort}/${oracle.databaseName}",
                'vertx.oracle.client.user': oracle.username,
                'vertx.oracle.client.password': oracle.password,
                'vertx.oracle.client.max-rows': '1'
        )

        when:
        int rows = applicationContext.getBean(Pool)
            .query("SELECT 1 FROM dual UNION ALL SELECT 2 FROM dual UNION ALL SELECT 3 FROM dual").execute()
            .map { result -> result.size() }
            .toCompletionStage().toCompletableFuture().get()

        then:
        rows == 1

        cleanup:
        applicationContext?.close()
    }
}
