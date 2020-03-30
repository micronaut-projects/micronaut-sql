package io.micronaut.configuration.jdbi;

import io.micronaut.transaction.annotation.ReadOnly;
import org.jdbi.v3.core.Jdbi;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.transaction.Transactional;

@Singleton
public class TxTestService {

    private final Jdbi db;

    public TxTestService(Jdbi db) {
        this.db = db;
    }

    @PostConstruct
    void init() {
        setupData();
    }

    @Transactional
    void setupData() {
        db.useHandle(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS foo(id INTEGER);");
            handle.execute("INSERT INTO foo(id) VALUES (0);");
        });
    }

    @ReadOnly
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

    @Transactional
    public void dropData() {
        db.useHandle(handle ->
                handle.execute("DELETE FROM foo")
        );
    }
}
