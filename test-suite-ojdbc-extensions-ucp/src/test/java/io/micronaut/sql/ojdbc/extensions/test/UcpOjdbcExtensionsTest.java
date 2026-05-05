package io.micronaut.sql.ojdbc.extensions.test;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import oracle.ucp.jdbc.PoolDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UcpOjdbcExtensionsTest implements TestPropertyProvider {

    @Test
    void dataSource(DataSource dataSource) throws SQLException {
        assertNotNull(dataSource);
        assertInstanceOf(PoolDataSource.class, dataSource);
        try (Connection connection = dataSource.getConnection()) {
            assertNotNull(connection);
        }
        assertTrue(FooBarPasswordProvider.invocations() > 0);
        assertEquals("9999-8888-7777", FooBarPasswordProvider.vaultId().toString());
    }

    @Override
    public @NonNull Map<String, String> getProperties() {
        FooBarPasswordProvider.reset();
        return Oracle.getProperties();
    }
}
