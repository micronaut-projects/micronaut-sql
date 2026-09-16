from typing import Annotated

from jakarta.inject import Singleton
from jakarta.persistence import EntityManager, PersistenceContext
from micronaut.transaction.annotation import Transactional

from micronaut.docs.hibernate.graalvm.Order import Order
from micronaut.docs.hibernate.graalvm.OrderId import OrderId


@Singleton
class OrderRepository:
    entity_manager: Annotated[EntityManager, PersistenceContext]

    @Transactional
    def save(self, id: OrderId, customer: str) -> Order:
        order = Order()
        order.id = id
        order.customer = customer
        self.entity_manager.persist(order)
        return order

    @Transactional(readOnly=True)
    def find_by_id(self, id: OrderId) -> Order | None:
        return self.entity_manager.find(Order, id)
