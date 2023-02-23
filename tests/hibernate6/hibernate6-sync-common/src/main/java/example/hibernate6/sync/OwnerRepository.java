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
package example.hibernate6.sync;

import example.domain.IOwner;
import example.sync.IOwnerRepository;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import jakarta.inject.Singleton;

import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

@Singleton
class OwnerRepository implements IOwnerRepository {

    private final EntityManager entityManager;

    OwnerRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public IOwner create() {
        return new Owner();
    }

    @TransactionalAdvice(propagation = TransactionDefinition.Propagation.MANDATORY)
    @Override
    public void save(IOwner entity) {
        entityManager.persist(entity);
    }

    @TransactionalAdvice(propagation = TransactionDefinition.Propagation.MANDATORY)
    @Override
    public void delete(IOwner entity) {
        entityManager.remove(entity);
    }

    @Override
    public IOwner findById(Long id) {
        return null;
    }

    @TransactionalAdvice(readOnly = true, propagation = TransactionDefinition.Propagation.MANDATORY)
    @Override
    public Collection<IOwner> findAll() {
        return new ArrayList<>(entityManager.createQuery("from Owner", Owner.class)
                .getResultList());
    }

    @TransactionalAdvice(readOnly = true, propagation = TransactionDefinition.Propagation.MANDATORY)
    @Override
    public Optional<Owner> findByName(String name) {
        return entityManager.createQuery("from Owner where name = :name", Owner.class)
                .setParameter("name", name)
                .getResultList()
                .stream()
                .findFirst();
    }
}
