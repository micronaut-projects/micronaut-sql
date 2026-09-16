from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.jdbc.multiple.InventoryService import InventoryService


@MicronautTest(transactional=False)
class MultipleDataSourcesTest:
    inventory_service: Annotated[InventoryService, Inject]

    @Test
    def test_qualified_data_sources_are_injected(self):
        assert "mem:default" in self.inventory_service.default_url()
        assert "mem:warehouse" in self.inventory_service.warehouse_url()
