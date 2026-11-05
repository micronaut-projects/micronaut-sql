from typing import Annotated

from io.vertx.sqlclient import Pool
from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from micronaut.docs.vertx.oracle.BookCatalog import BookCatalog


# The Oracle Free container properties are supplied by the Java example.vertx.oracle.OracleTestConfigurer
# of the "vertx-oracle" environment: Micronaut Test calls TestPropertyProvider before the context, and with it the
# GraalPy runtime, exists, so a Python test class cannot provide them.
@MicronautTest(transactional=False, environments=["vertx-oracle"])
class BookCatalogTest:
    pool: Annotated[Pool, Inject]
    book_catalog: Annotated[BookCatalog, Inject]

    @Test
    def finds_a_title_through_the_vertx_oracle_client(self):
        (self.pool.query("CREATE TABLE book (id NUMBER PRIMARY KEY, title VARCHAR2(100))").execute()
            .compose(lambda ignored: self.pool.query("INSERT INTO book VALUES (1, 'The Stand')").execute())
            .toCompletionStage().toCompletableFuture().get())

        assert self.book_catalog.find_title(1).toCompletableFuture().get() == "The Stand"
