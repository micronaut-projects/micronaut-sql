package io.micronaut.docs.jdbc.refreshdiff;

import com.zaxxer.hikari.HikariDataSource;
import io.micronaut.context.annotation.Property;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "spec.name", value = "DbPasswordDiffRefresherTest")
@MicronautTest(transactional = false)
class DbPasswordRefresherTest {

    @Inject
    DbPasswordRefresher refresher;

    @Inject
    DataSource dataSource;

    @Inject
    DataSourceResolver dataSourceResolver;

    @Test
    void testPasswordRotation() throws SQLException {
        assertEquals("initial", hikari().getPassword());

        // the secret service rotates the password: the "db-password" configuration value changes
        alterPassword("rotated");
        System.setProperty("db-password", "rotated");

        refresher.refresh();

        assertEquals("rotated", hikari().getPassword());
        try (Connection connection = hikari().getConnection()) {
            assertEquals("SA", connection.getMetaData().getUserName());
        }
    }

    @AfterEach
    void restorePassword() throws SQLException {
        alterPassword("initial");
        System.clearProperty("db-password");
        refresher.refresh();
    }

    private HikariDataSource hikari() {
        return (HikariDataSource) dataSourceResolver.resolve(dataSource); // unwrap the transaction-aware DataSource
    }

    private void alterPassword(String password) throws SQLException {
        try (Connection connection = hikari().getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("ALTER USER sa SET PASSWORD '" + password + "'");
        }
    }
}
