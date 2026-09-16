# tag::imports[]
from typing import Annotated

from jakarta.persistence import EmbeddedId, Entity, Table

from micronaut.docs.hibernate.graalvm.OrderId import OrderId
# end::imports[]


# tag::clazz[]
@Entity
@Table(name="orders")
class Order:
    id: Annotated[OrderId | None, EmbeddedId] = None
    customer: str | None = None
# end::clazz[]
