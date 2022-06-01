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
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Controller("/pets")
class PetController {

    private final Stage.SessionFactory sessionFactory;
    private final Mapper mapper;

    PetController(SessionFactory sessionFactory, Mapper mapper) {
        this.sessionFactory = sessionFactory.unwrap(Stage.SessionFactory.class);
        this.mapper = mapper;
    }

    @Get
    CompletionStage<List<PetDto>> all() {
        return sessionFactory.withSession(session -> session.createQuery("from Pet", Pet.class)
                .getResultList()
                .thenCompose(pets -> {
                    // HR000037: Reactive sessions do not support transparent lazy fetching
                    // - use Session.fetch() (entity 'example.domain.Owner' with id '1' was not loaded)
                    CompletableFuture<List<Pet>> result = CompletableFuture.completedFuture(new ArrayList<>());
                    for (Pet pet : pets) {
                        result = result.thenCombine(session.fetch(pet.getOwner()), (newPets, owner) -> {
                            pet.setOwner(owner);
                            newPets.add(pet);
                            return newPets;
                        });
                    }
                    return result;
                })
                .thenApply(pets -> pets.stream()
                        .map(mapper::toPetDto)
                        .collect(Collectors.toList())));
    }

    @Get("/{name}")
    CompletionStage<PetDto> byName(String name) {
        return sessionFactory.withSession(session -> session.createQuery("from Pet where name = :name", Pet.class)
                .setParameter("name", name)
                .getResultList()
                .thenCompose(pets -> {
                    Pet pet = pets.stream().findFirst().orElse(null);
                    if (Hibernate.isInitialized(pet.getOwner())) {
                        throw new IllegalStateException("Expected not initialized owner ref");
                    }
                    // HR000037: Reactive sessions do not support transparent lazy fetching
                    // - use Session.fetch() (entity 'example.domain.Owner' with id '1' was not loaded)
                    return session.fetch(pet.getOwner()).thenApply(owner -> {
                        pet.setOwner(owner);
                        return pet;
                    }).thenApply(petWithOwner -> {
                        PetDto petDto = mapper.toPetDto(petWithOwner);
                        if (!Hibernate.isInitialized(petWithOwner.getOwner())) {
                            throw new IllegalStateException("Expected initialized owner ref");
                        }
                        return petDto;
                    });
                })
        );
    }

}