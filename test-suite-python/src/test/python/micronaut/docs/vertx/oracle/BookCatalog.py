# tag::imports[]
from typing import Annotated

from io.vertx.sqlclient import Pool, Tuple
from jakarta.inject import Inject, Singleton
from java.util.concurrent import CompletionStage
# end::imports[]


# tag::clazz[]
@Singleton
class BookCatalog:
    pool: Annotated[Pool, Inject]  # <1>

    def find_title(self, id: int) -> CompletionStage:
        return (self.pool.preparedQuery("SELECT title FROM book WHERE id = ?")  # <2>
                .execute(Tuple.of(id))
                .map(lambda rows: rows.iterator().next().getString("TITLE"))
                .toCompletionStage())
# end::clazz[]
