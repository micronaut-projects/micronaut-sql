package io.micronaut.docs.jdbc.multiple

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class MultipleDataSourcesTest {

    @Inject
    lateinit var inventoryService: InventoryService

    @Test
    fun testQualifiedDataSourcesAreInjected() {
        assertTrue(inventoryService.defaultUrl().contains("mem:default"))
        assertTrue(inventoryService.warehouseUrl().contains("mem:warehouse"))
    }
}
