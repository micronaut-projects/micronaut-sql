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
package io.micronaut.configuration.hibernate.jpa;

import io.micronaut.transaction.annotation.TransactionalAdvice;
import jakarta.inject.Singleton;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Singleton
public class JavaBookService {

    @PersistenceContext
    EntityManager entityManagerField;


    private EntityManager entityManager;

    @PersistenceContext
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @TransactionalAdvice
    public boolean testFieldInject() {
        entityManagerField.clear();
        return true;
    }

    @TransactionalAdvice
    public boolean testMethodInject() {
        entityManager.clear();
        return true;
    }

    @TransactionalAdvice
    public boolean testNativeQuery() {
        // just testing the method can be invoked
        entityManager.createNativeQuery("select * from book", Book.class).getResultList();
        return true;
    }

    @TransactionalAdvice
    public boolean testClose() throws Exception {
        // just testing the method can be invoked
        ((AutoCloseable)entityManager).close();
        return true;
    }
}
