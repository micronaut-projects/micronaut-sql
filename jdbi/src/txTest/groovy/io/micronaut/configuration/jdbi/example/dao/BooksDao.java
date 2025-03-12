package io.micronaut.configuration.jdbi.example.dao;

import io.micronaut.configuration.jdbi.example.model.Book;
import io.micronaut.transaction.TransactionOperations;
import io.micronaut.transaction.TransactionStatus;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.jdbi.v3.sqlobject.statement.UseRowMapper;
import org.jdbi.v3.sqlobject.transaction.Transaction;

import java.util.List;

public interface BooksDao {

    @SqlQuery("SELECT * FROM Books")
    @UseRowMapper(BookMapper.class)
    @Transaction(TransactionIsolationLevel.READ_COMMITTED)
    List<Book> listAll();

    @SqlQuery("SELECT * FROM Books")
    @UseRowMapper(BookMapper.class)
    List<Book> listNoTransaction();

    @SqlUpdate("""
                INSERT INTO Books (ID, NAME) VALUES (:id, :name)
        """)
    public void create(@BindBean Book book);

    @Transaction
    default Object captureMapperTransaction(TransactionOperations<?> ops) {
        BookMapper.transactionOperations = ops;
        TransactionStatus<?> status = ops.findTransactionStatus().orElse(null);
        if (status == null) {
            throw new IllegalStateException("Missing transaction status");
        }
        listAll();
        return ops.findTransactionStatus().get();
    }
}
