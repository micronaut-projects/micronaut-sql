package io.micronaut.docs.jdbc.refresh

import com.zaxxer.hikari.HikariDataSource
import io.micronaut.context.annotation.Property
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.sql.DataSource

@Property(name = "spec.name", value = "DbPasswordRefresherTest")
@MicronautTest(transactional = false)
class DbPasswordRefresherTest {

    @Inject
    lateinit var refresher: DbPasswordRefresher

    @Inject
    lateinit var secretStore: DbSecretStore

    @Inject
    lateinit var dataSource: DataSource

    @Inject
    lateinit var dataSourceResolver: DataSourceResolver

    @Test
    fun testPasswordRotation() {
        assertEquals("initial", hikari().password)

        // the secret service rotates the password, the configuration ("${db-password}") now resolves to the new value
        alterPassword("rotated")
        System.setProperty("db-password", "rotated")
        secretStore.rotate("rotated")

        refresher.refresh()

        assertEquals("rotated", hikari().password)
        hikari().connection.use { connection ->
            assertEquals("SA", connection.metaData.userName)
        }
    }

    @AfterEach
    fun restorePassword() {
        alterPassword("initial")
        System.clearProperty("db-password")
        secretStore.rotate("initial")
        refresher.refresh()
    }

    private fun hikari(): HikariDataSource =
        dataSourceResolver.resolve(dataSource) as HikariDataSource // unwrap the transaction-aware DataSource

    private fun alterPassword(password: String) {
        hikari().connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("ALTER USER sa SET PASSWORD '$password'")
            }
        }
    }
}
