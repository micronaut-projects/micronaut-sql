from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.jdbc.transactions.Book import Book
from micronaut.docs.jdbc.transactions.BookRepository import BookRepository


@MicronautTest(transactional=False)
class BookRepositoryTest:
    book_repository: Annotated[BookRepository, Inject]

    @Test
    def test_save_book_in_transaction(self):
        self.book_repository.create_table()
        before = self.book_repository.count()
        self.book_repository.save_book(Book("The Stand", 1000))
        assert self.book_repository.count() == before + 1
