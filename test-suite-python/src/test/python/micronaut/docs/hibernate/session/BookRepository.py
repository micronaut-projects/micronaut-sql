# tag::imports[]
from typing import Annotated

from jakarta.inject import Singleton
from jakarta.persistence import EntityManager, PersistenceContext
from micronaut.transaction.annotation import Transactional

from micronaut.docs.hibernate.session.Book import Book
# end::imports[]


# tag::clazz[]
@Singleton
class BookRepository:
    entity_manager: Annotated[EntityManager, PersistenceContext]  # <1>
    other_manager: Annotated[EntityManager, PersistenceContext(name="other")]  # <2>

    @Transactional  # <3>
    def save(self, title: str) -> Book:
        book = Book(title)
        self.entity_manager.persist(book)
        return book

    @Transactional("other")  # <4>
    def save_to_other(self, title: str) -> Book:
        book = Book(title)
        self.other_manager.persist(book)
        return book

    @Transactional(readOnly=True)
    def find_by_id(self, id: int) -> Book | None:
        return self.entity_manager.find(Book, id)

    @Transactional(value="other", readOnly=True)
    def find_in_other_by_id(self, id: int) -> Book | None:
        return self.other_manager.find(Book, id)
# end::clazz[]
