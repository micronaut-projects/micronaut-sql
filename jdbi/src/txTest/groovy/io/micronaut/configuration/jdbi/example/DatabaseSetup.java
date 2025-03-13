package io.micronaut.configuration.jdbi.example;

import io.micronaut.configuration.jdbi.example.dao.BooksDao;
import io.micronaut.configuration.jdbi.example.model.Book;
import io.micronaut.data.connection.annotation.Connectable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

@Singleton
public class DatabaseSetup {
    private final BooksDao booksDao;
    private final Jdbi jdbi;

    @Inject
    public DatabaseSetup(@Named("nonTransactional") BooksDao booksDao, Jdbi jdbi) {
        this.booksDao = booksDao;
        this.jdbi = jdbi;
    }

    @Connectable
    public void initialize() {
        try (Handle open = jdbi.open()) {
            open.execute("create table books (id int primary key, name text)");
        }
    }

    @Connectable
    public void drop() {
        try (Handle open = jdbi.open()) {
            open.execute("drop table books");
        }
    }

    public void fillInitialRecords() {
        Book b1 = new Book(1, "A");
        Book b2 = new Book(2, "B");
        Book b3 = new Book(3, "C");

        booksDao.create(b1);
        booksDao.create(b2);
        booksDao.create(b3);
    }
}
