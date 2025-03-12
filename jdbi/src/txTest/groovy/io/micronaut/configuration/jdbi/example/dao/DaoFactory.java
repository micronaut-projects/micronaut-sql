package io.micronaut.configuration.jdbi.example.dao;

import io.micronaut.context.annotation.Factory;
import io.micronaut.data.connection.annotation.Connectable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import org.jdbi.v3.core.Jdbi;

@Factory
public class DaoFactory {
    @Singleton
    @Connectable
    @Named("nonTransactional")
    BooksDao createBookDao(Jdbi jdbi) {
        return jdbi.onDemand(BooksDao.class);
    }

    @Singleton
    @Connectable
    @Named("transactions")
    @Transactional
    BooksDao createTxBookDao(Jdbi jdbi) {
        return jdbi.onDemand(BooksDao.class);
    }
}
