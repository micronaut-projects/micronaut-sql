from typing import Annotated

from jakarta.persistence import Entity, GeneratedValue, Id
from java.lang import Long


@Entity
class Book:
    id: Annotated[Long | None, Id, GeneratedValue] = None
    title: str | None = None

    def __init__(self, title: str | None = None):
        self.title = title
