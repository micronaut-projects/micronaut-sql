from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Disabled, Test

from micronaut.docs.hibernate.session.BookRepository import BookRepository


@Disabled("TODO(python): the id Hibernate assigns on persist is set on the Java wrapper of the Python entity and not written back to the Python object (book.id stays None), see DISABLED_TESTS.md")
@MicronautTest(transactional=False)
class BookRepositoryTest:
    book_repository: Annotated[BookRepository, Inject]

    @Test
    def test_injected_entity_managers(self):
        book = self.book_repository.save("The Stand")
        assert book.id is not None
        assert self.book_repository.find_by_id(book.id).title == "The Stand"
        assert self.book_repository.find_in_other_by_id(book.id) is None

        other = self.book_repository.save_to_other("It")
        assert other.id is not None
        assert self.book_repository.find_in_other_by_id(other.id).title == "It"
