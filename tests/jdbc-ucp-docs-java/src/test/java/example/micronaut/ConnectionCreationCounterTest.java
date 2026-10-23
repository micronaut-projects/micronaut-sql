package example.micronaut;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
class ConnectionCreationCounterTest {

    @Inject
    DataSource dataSource;

    @Inject
    ConnectionCreationCounter counter;

    @Test
    void customizerSeesCreatedConnections() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertTrue(connection.isValid(1));
        }
        assertTrue(counter.getCreated() > 0);
    }
}
