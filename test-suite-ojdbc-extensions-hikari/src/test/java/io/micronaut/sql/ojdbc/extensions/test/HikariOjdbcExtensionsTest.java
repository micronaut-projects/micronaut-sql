package io.micronaut.sql.ojdbc.extensions.test;

import io.micronaut.configuration.jdbc.hikari.HikariUrlDataSource;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.micronaut.test.support.TestPropertyProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.sql.DataSource;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(startApplication = false)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HikariOjdbcExtensionsTest implements TestPropertyProvider {

    @Test
    void dataSource(DataSource dataSource) {
        assertNotNull(dataSource);
        assertInstanceOf(HikariUrlDataSource.class, dataSource);
        HikariUrlDataSource hikariUrlDataSource = (HikariUrlDataSource) dataSource;
        assertFalse(hikariUrlDataSource.getDataSourceProperties().isEmpty());
        assertTrue(hikariUrlDataSource.getDataSourceProperties().containsKey("oracle.jdbc.provider.password"));
        assertEquals("example-provider", hikariUrlDataSource.getDataSourceProperties().get("oracle.jdbc.provider.password"));
    }

    @Override
    public @NonNull Map<String, String> getProperties() {
        return Oracle.getProperties();
    }
}
