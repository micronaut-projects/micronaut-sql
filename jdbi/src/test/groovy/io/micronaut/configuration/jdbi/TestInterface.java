package io.micronaut.configuration.jdbi;

import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.transaction.Transaction;

public interface TestInterface {
    @SqlQuery("SELECT COUNT(*) FROM foo")
    @Transaction
    Integer count();
}
