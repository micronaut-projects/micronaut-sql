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
package example.sync;

import example.domain.IPet;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import jakarta.inject.Singleton;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.Optional;

@Singleton
class PetRepository implements IPetRepository {

    private final EntityManager entityManager;

    PetRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public IPet create() {
        return new Pet();
    }

    @TransactionalAdvice(propagation = TransactionDefinition.Propagation.MANDATORY)
    @Override
    public void save(IPet pet) {
        entityManager.persist(pet);
    }

    @TransactionalAdvice(propagation = TransactionDefinition.Propagation.MANDATORY)
    @Override
    public void delete(IPet pet) {
        entityManager.remove(pet);
    }

    @Override
    public Collection<Pet> findAll() {
        return entityManager.createQuery("from Pet", Pet.class).getResultList();
    }

    @Override
    public Optional<IPet> findByName(String name) {
        return entityManager.createQuery("from Pet where name = :name", Pet.class)
                .setParameter("name", name)
                .getResultList()
                .stream()
                .findFirst()
                .map(pet -> {
                    if (Hibernate.isInitialized(pet.getOwner())) {
                        throw new IllegalStateException("Expected not initialized owner ref");
                    }
                    pet.getOwner().getName();
                    if (!Hibernate.isInitialized(pet.getOwner())) {
                        throw new IllegalStateException("Expected initialized owner ref");
                    }
                    return pet;
                });
    }
}
