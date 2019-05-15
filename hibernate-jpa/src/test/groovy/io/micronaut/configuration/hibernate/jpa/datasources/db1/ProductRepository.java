package io.micronaut.configuration.hibernate.jpa.datasources.db1;

import io.micronaut.configuration.hibernate.jpa.scope.CurrentSession;
import io.micronaut.spring.tx.annotation.Transactional;

import javax.inject.Singleton;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@Transactional
@Singleton
public class ProductRepository {

    private final EntityManager entityManager;

    public ProductRepository(@CurrentSession EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Product save(@NotBlank String isdn, @NotBlank String name) {
        Product book = new Product(isdn, name);
        entityManager.persist(book);
        return book;
    }

    @Transactional(readOnly = true) // <3>
    public Optional<Product> findById(@NotNull Long id) {
        return Optional.ofNullable(entityManager.find(Product.class, id));
    }
}
