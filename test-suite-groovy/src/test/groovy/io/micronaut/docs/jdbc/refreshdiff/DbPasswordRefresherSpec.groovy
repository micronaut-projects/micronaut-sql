package io.micronaut.docs.jdbc.refreshdiff

import com.zaxxer.hikari.HikariDataSource
import io.micronaut.context.annotation.Property
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.Statement

@Property(name = "spec.name", value = "DbPasswordDiffRefresherSpec")
@MicronautTest(transactional = false)
class DbPasswordRefresherSpec extends Specification {

    @Inject
    DbPasswordRefresher refresher

    @Inject
    DataSource dataSource

    @Inject
    DataSourceResolver dataSourceResolver

    void "test password rotation"() {
        expect:
        hikari.password == "initial"

        when: "the secret service rotates the password: the 'db-password' configuration value changes"
        alterPassword("rotated")
        System.setProperty("db-password", "rotated")
        refresher.refresh()

        then:
        hikari.password == "rotated"
        hikari.connection.withCloseable { Connection connection -> connection.metaData.userName } == "SA"
    }

    void cleanup() {
        alterPassword("initial")
        System.clearProperty("db-password")
        refresher.refresh()
    }

    private HikariDataSource getHikari() {
        (HikariDataSource) dataSourceResolver.resolve(dataSource) // unwrap the transaction-aware DataSource
    }

    private void alterPassword(String password) {
        try (Connection connection = hikari.connection
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER USER sa SET PASSWORD '" + password + "'")
        }
    }
}
