package io.micronaut.configuration.jdbi.example.dao;

import io.micronaut.configuration.jdbi.example.model.Book;
import io.micronaut.transaction.TransactionOperations;
import org.jdbi.v3.core.mapper.RowMapper;
import org.jdbi.v3.core.statement.StatementContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.Semaphore;

public class BookMapper implements RowMapper<Book> {
    /**
     * Allow the execution to stop, retaining Handle and its Connection
     * opened.
     */
    public static final ThreadLocal<Semaphore>  locker = new ThreadLocal<>();
    public static final ThreadLocal<Semaphore> toRelease = new ThreadLocal<>();
    public static volatile TransactionOperations<?> transactionOperations;
    public static Object mapperTransactionObject;
    public static Object daoTransactionObject;

    @Override
    public Book map(ResultSet rs, StatementContext ctx) throws SQLException {
        Semaphore s = locker.get();
        Semaphore s2 = toRelease.get();
        try {
            // block the resultset processing, so a concurrent thread may step in
            if (s2 != null) {
                s2.release(100);
            }
            if (s != null) {
                s.acquire();
            }
        } catch (InterruptedException e) {
            // no op
        }
        if (transactionOperations != null) {
            mapperTransactionObject = BookMapper.transactionOperations.findTransactionStatus().
                get().getTransaction();
        }
        return new Book(rs.getInt("id"), rs.getString("name"));
    }
}
