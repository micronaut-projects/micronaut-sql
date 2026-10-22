package io.micronaut.sql.ojdbc.extensions.test;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The URL names an Oracle JDBC {@code config-file} provider, which supplies the connect descriptor,
 * user and password. The data source configuration has only the URL.
 */
@MicronautTest(startApplication = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UcpConfigFileProviderTest implements TestPropertyProvider {

    @Test
    void connectsWithTheUserFromTheConfigurationFile(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT USER FROM DUAL")) {
            assertTrue(resultSet.next());
            assertEquals("TEST", resultSet.getString(1));
        }
    }

    @Override
    public @NonNull Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>(Oracle.getConfigFileProperties());
        // a distinct pool name, since UcpOjdbcExtensionsTest registers "default" in the same UCP manager
        properties.put("datasources.default.connection-pool-name", "configFile");
        return properties;
    }
}
