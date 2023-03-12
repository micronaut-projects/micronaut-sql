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
package io.micronaut.configuration.hibernate6.jpa.datasources.db2;

import io.micronaut.transaction.annotation.TransactionalAdvice;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import jakarta.persistence.EntityManager;
import javax.transaction.Transactional;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Optional;

@Transactional
@TransactionalAdvice("db2")
@Singleton
public class BookstoreRepository {

    private final EntityManager entityManager;

    public BookstoreRepository(@Named("db2") EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Bookstore save(@NotBlank String name) {
        Bookstore bookstore = new Bookstore(name);
        entityManager.persist(bookstore);
        return bookstore;
    }

    @Transactional
    @TransactionalAdvice(readOnly = true) // <3>
    public Optional<Bookstore> findById(@NotNull Long id) {
        return Optional.ofNullable(entityManager.find(Bookstore.class, id));
    }
}
