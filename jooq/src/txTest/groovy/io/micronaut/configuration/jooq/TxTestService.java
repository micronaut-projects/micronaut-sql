package io.micronaut.configuration.jooq;

import io.micronaut.transaction.annotation.ReadOnly;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.transaction.Transactional;

@Singleton
public class TxTestService {

    private final DSLContext db;

    public TxTestService(DSLContext db) {
        this.db = db;
    }

    @PostConstruct
    public void init() {
        runInit();
    }

    @Transactional
    public void runInit() {
        db.execute("CREATE TABLE IF NOT EXISTS foo(id INTEGER);");
        db.execute("INSERT INTO foo(id) VALUES (0);");
    }

    @ReadOnly
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
