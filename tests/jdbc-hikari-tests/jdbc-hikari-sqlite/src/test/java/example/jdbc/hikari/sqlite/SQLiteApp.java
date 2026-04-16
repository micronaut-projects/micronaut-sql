package example.jdbc.hikari.sqlite;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import io.micronaut.testresources.client.TestResourcesClient;
import io.micronaut.testresources.client.TestResourcesClientFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

@MicronautTest(transactional = false)
@Property(name = "datasources.default.db-type", value = "sqlite")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLiteApp implements TestPropertyProvider {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ApplicationContext context;

    @Inject
    DataSource dataSource;

    @Inject
    DataSourceResolver dataSourceResolver;

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("micronaut.test.resources.scope", getClass().getName());
        return properties;
    }

    @BeforeAll
    void init() {
        HttpResponse<Void> response = client.toBlocking().exchange(HttpRequest.GET("/init"));
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @AfterAll
    void cleanup() {
        try {
            HttpResponse<Void> response = client.toBlocking().exchange(HttpRequest.GET("/destroy"));
            assertEquals(HttpStatus.OK, response.getStatus());
        } finally {
            try {
                TestResourcesClient testResourcesClient = TestResourcesClientFactory.extractFrom(context);
                testResourcesClient.closeScope(getClass().getName());
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Test
    void shouldInitializeOwners() throws SQLException {
        try (Connection connection = dataSourceResolver.resolve(dataSource).getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM owners")) {
            assertEquals(2, singleLong(resultSet));
        }
    }

    @Test
    void shouldInitializePets() throws SQLException {
        try (Connection connection = dataSourceResolver.resolve(dataSource).getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM pets")) {
            assertEquals(3, singleLong(resultSet));
        }
    }

    @Test
    void shouldPersistOwnerNames() throws SQLException {
        try (Connection connection = dataSourceResolver.resolve(dataSource).getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT name FROM owners ORDER BY id")) {
            resultSet.next();
            assertEquals("Fred", resultSet.getString(1));
            resultSet.next();
            assertEquals("Barney", resultSet.getString(1));
        }
    }

    private long singleLong(ResultSet resultSet) throws SQLException {
        resultSet.next();
        return resultSet.getLong(1);
    }
}
