package io.micronaut.docs.jdbc.multiple

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(transactional = false)
class MultipleDataSourcesSpec extends Specification {

    @Inject
    InventoryService inventoryService

    void "test qualified data sources are injected"() {
        expect:
        inventoryService.defaultUrl().contains("mem:default")
        inventoryService.warehouseUrl().contains("mem:warehouse")
    }
}
