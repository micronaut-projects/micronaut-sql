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
package example.controllers;

import example.controllers.dto.PetDto;
import example.domain.Pet;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import org.hibernate.Hibernate;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller("/pets")
class PetController {

    private final EntityManager entityManager;
    private final Mapper mapper;

    PetController(EntityManager entityManager, Mapper mapper) {
        this.entityManager = entityManager;
        this.mapper = mapper;
    }

    @TransactionalAdvice(readOnly = true)
    @Get
    List<PetDto> all() {
        List<Pet> from_pet = entityManager.createQuery("from Pet", Pet.class)
                .getResultList();
        return from_pet
                .stream()
                .map(mapper::toPetDto)
                .collect(Collectors.toList());
    }

    @TransactionalAdvice(readOnly = true)
    @Get("/{name}")
    Optional<PetDto> byName(String name) {
        return entityManager.createQuery("from Pet where name = :name", Pet.class)
                .setParameter("name", name)
                .getResultList()
                .stream()
                .findFirst()
                .map(pet -> {
                    if (Hibernate.isInitialized(pet.getOwner())) {
                        throw new IllegalStateException("Expected not initialized owner ref");
                    }
                    PetDto petDto = mapper.toPetDto(pet);
                    if (!Hibernate.isInitialized(pet.getOwner())) {
                        throw new IllegalStateException("Expected initialized owner ref");
                    }
                    return petDto;
                });
    }

}