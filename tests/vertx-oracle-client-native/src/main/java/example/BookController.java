package example;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.StreamSupport;

@Controller("/books")
final class BookController {

    private final Pool pool;

    BookController(Pool pool) {
        this.pool = pool;
    }

    @Post("/setup")
    CompletionStage<String> setup() {
        return pool.query("CREATE TABLE book (id NUMBER PRIMARY KEY, title VARCHAR2(100), published TIMESTAMP)").execute()
            .compose(created -> pool.preparedQuery("INSERT INTO book (id, title, published) VALUES (?, ?, ?)")
                .executeBatch(List.of(
                    Tuple.of(1, "The Stand", java.time.LocalDateTime.of(1978, 10, 3, 0, 0)),
                    Tuple.of(2, "Dune", java.time.LocalDateTime.of(1965, 8, 1, 0, 0)))))
            .map(inserted -> "created")
            .toCompletionStage();
    }

    @Get
    CompletionStage<List<String>> titles() {
        return pool.preparedQuery("SELECT title, published FROM book WHERE id >= ? ORDER BY id").execute(Tuple.of(1))
            .map(rows -> StreamSupport.stream(rows.spliterator(), false)
                .map(BookController::describe)
                .toList())
            .toCompletionStage();
    }

    private static String describe(Row row) {
        return row.getString("TITLE") + " (" + row.getLocalDateTime("PUBLISHED").getYear() + ")";
    }
}
