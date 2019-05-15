package io.micronaut.configuration.hibernate.jpa.datasources.db2;

import io.micronaut.configuration.hibernate.jpa.scope.CurrentSession;
import io.micronaut.context.annotation.Context;
import io.micronaut.spring.tx.annotation.Transactional;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Transactional("db2")
@Singleton
public class BookstoreRepository {

    private final EntityManager entityManager;

    public BookstoreRepository(@CurrentSession("db2") EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Bookstore save(@NotBlank String name) {
        Bookstore bookstore = new Bookstore(name);
        entityManager.persist(bookstore);
        return bookstore;
    }

    @Transactional(readOnly = true) // <3>
    public Optional<Bookstore> findById(@NotNull Long id) {
        return Optional.ofNullable(entityManager.find(Bookstore.class, id));
    }
}
