# tag::imports[]
from typing import Annotated

from jakarta.persistence import Entity, FetchType, GeneratedValue, Id, ManyToOne
from java.lang import Long

from micronaut.docs.hibernate.proxies.Owner import Owner
# end::imports[]


# tag::clazz[]
@Entity
class Pet:
    id: Annotated[Long | None, Id, GeneratedValue] = None
    name: str | None = None
    owner: Annotated[Owner | None, ManyToOne(fetch=FetchType.LAZY)] = None
# end::clazz[]
