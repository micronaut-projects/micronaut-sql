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
package io.micronaut.configuration.hibernate.jpa.mapping;

import io.micronaut.core.annotation.Nullable;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import jakarta.inject.Singleton;

import jakarta.persistence.EntityManager;
import javax.validation.constraints.NotBlank;

@TransactionalAdvice
@Singleton
public class AccountRepository {

    private final EntityManager entityManager;

    public AccountRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public Account create(@NotBlank String username, @NotBlank String password) {
        Account account = new Account(username, password);
        entityManager.persist(account);
        return account;
    }

    @Nullable
    public Account findByUsername(String username) {
        return (Account) entityManager
                .createNativeQuery("SELECT * FROM custom_view WHERE username = :username", Account.class)
                .setParameter("username", username)
                .getSingleResult();
    }

}
