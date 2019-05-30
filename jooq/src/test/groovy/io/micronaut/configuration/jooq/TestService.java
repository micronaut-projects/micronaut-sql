package io.micronaut.configuration.jooq;

import io.micronaut.spring.tx.annotation.Transactional;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import javax.inject.Singleton;

@Singleton
public class TestService {

    private final DSLContext db;

    public TestService(DSLContext db) {
        this.db = db;
        db.execute("CREATE TABLE IF NOT EXISTS foo(id INTEGER);");
        db.execute("INSERT INTO foo(id) VALUES (0);");
    }

    public int count() {
        return db.fetchCount(DSL.table("foo"));
    }

    @Transactional
    public void abortTransaction() {
        db.execute("INSERT INTO foo(id) VALUES (1);");
        int count = count();
        throw new RuntimeException("count=" + count);
    }

    @Transactional
    public void commitTransaction() {
        db.execute("INSERT INTO foo(id) VALUES (1);");
    }

}
