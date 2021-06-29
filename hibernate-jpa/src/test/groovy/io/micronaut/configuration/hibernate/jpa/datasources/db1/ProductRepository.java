/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.hibernate.jpa.datasources.db1;

import io.micronaut.configuration.hibernate.jpa.scope.CurrentSession;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import jakarta.inject.Singleton;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Optional;

@TransactionalAdvice
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

    @TransactionalAdvice(readOnly = true) // <3>
    public Optional<Product> findById(@NotNull Long id) {
        return Optional.ofNullable(entityManager.find(Product.class, id));
    }
}
