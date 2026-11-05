package io.micronaut.docs.vertx.oracle;

// tag::imports[]
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Tuple;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletionStage;
// end::imports[]

// tag::clazz[]
@Singleton
public class BookCatalog {

    private final Pool pool; // <1>

    public BookCatalog(Pool pool) {
        this.pool = pool;
    }

    public CompletionStage<String> findTitle(long id) {
        return pool.preparedQuery("SELECT title FROM book WHERE id = ?") // <2>
            .execute(Tuple.of(id))
            .map(rows -> rows.iterator().next().getString("TITLE"))
            .toCompletionStage();
    }
}
// end::clazz[]
