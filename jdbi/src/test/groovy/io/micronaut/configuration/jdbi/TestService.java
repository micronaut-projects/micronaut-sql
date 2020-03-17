package io.micronaut.configuration.jdbi;

import io.micronaut.spring.tx.annotation.Transactional;
import org.jdbi.v3.core.Jdbi;

import javax.inject.Singleton;

@Singleton
public class TestService {

    private final Jdbi db;

    public TestService(Jdbi db) {
        this.db = db;
        db.useHandle(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS foo(id INTEGER);");
            handle.execute("INSERT INTO foo(id) VALUES (0);");
        });

    }

    public int count() {
        return db.withHandle(handle ->
            handle.createQuery("SELECT COUNT(*) FROM foo").mapTo(Integer.class).one()
        );
    }

    @Transactional
    public void abortTransaction() {
        db.useHandle(handle -> {
            handle.execute("INSERT INTO foo(id) VALUES (1);");
            int count = count();
            throw new RuntimeException("count=" + count);
        });
    }

    @Transactional
    public void commitTransaction() {
        db.useHandle(handle ->
            handle.execute("INSERT INTO foo(id) VALUES (1);")
        );
    }

}
