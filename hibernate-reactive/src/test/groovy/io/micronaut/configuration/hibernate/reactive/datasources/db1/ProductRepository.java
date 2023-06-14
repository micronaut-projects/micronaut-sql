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
package io.micronaut.configuration.hibernate.reactive.datasources.db1;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;

import jakarta.persistence.EntityManager;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

@Transactional
@Singleton
public class ProductRepository {

    private final EntityManager entityManager;

    public ProductRepository(EntityManager entityManager) {
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
