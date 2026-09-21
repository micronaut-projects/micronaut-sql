from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.hibernate.graalvm.OrderId import OrderId
from micronaut.docs.hibernate.graalvm.OrderRepository import OrderRepository


@MicronautTest(transactional=False)
class EmbeddedIdTest:
    order_repository: Annotated[OrderRepository, Inject]

    @Test
    def test_embedded_id(self):
        id = OrderId("EU", 42)
        self.order_repository.save(id, "Fred")
        order = self.order_repository.find_by_id(OrderId("EU", 42))
        assert order.customer == "Fred"
        assert order.id.region == "EU"
        assert order.id.number == 42
