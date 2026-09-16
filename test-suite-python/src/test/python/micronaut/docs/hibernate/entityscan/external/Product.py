from typing import Annotated

from jakarta.persistence import Entity, GeneratedValue, Id
from java.lang import Long


@Entity
class Product:
    id: Annotated[Long | None, Id, GeneratedValue] = None
    name: str | None = None
