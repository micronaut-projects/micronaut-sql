package io.micronaut.docs.jdbc.multiple;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
class MultipleDataSourcesTest {

    @Inject
    InventoryService inventoryService;

    @Test
    void testQualifiedDataSourcesAreInjected() throws SQLException {
        assertTrue(inventoryService.defaultUrl().contains("mem:default"));
        assertTrue(inventoryService.warehouseUrl().contains("mem:warehouse"));
    }
}
