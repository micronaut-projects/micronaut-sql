package io.micronaut.configuration.hibernate.jpa.datasources.db2;

import io.micronaut.configuration.hibernate.jpa.scope.CurrentSession;
import io.micronaut.context.annotation.Context;
import io.micronaut.spring.tx.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Context
public class BookstoreMethodLevelTransaction {

    private final EntityManager entityManager;

    public BookstoreMethodLevelTransaction(@CurrentSession("db2") EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional(transactionManager = "db2")
    public Bookstore save(@NotBlank String name) {
        Bookstore bookstore = new Bookstore(name);
        entityManager.persist(bookstore);
        return bookstore;
    }

    @Transactional(readOnly = true, transactionManager = "db2")
    public Optional<Bookstore> findById(@NotNull Long id) {
        return Optional.ofNullable(entityManager.find(Bookstore.class, id));
    }
}
