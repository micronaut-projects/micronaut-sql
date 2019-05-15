package io.micronaut.configuration.hibernate.jpa.datasources.db1;

import io.micronaut.configuration.hibernate.jpa.scope.CurrentSession;
import io.micronaut.context.annotation.Context;
import io.micronaut.spring.tx.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Transactional
@Context
public class BookRepository {

    private final EntityManager entityManager;

    public BookRepository(@CurrentSession EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Book save(@NotBlank String isdn, @NotBlank String name) {
        Book book = new Book(isdn, name);
        entityManager.persist(book);
        return book;
    }

    @Transactional(readOnly = true) // <3>
    public Optional<Book> findById(@NotNull Long id) {
        return Optional.ofNullable(entityManager.find(Book.class, id));
    }
}
