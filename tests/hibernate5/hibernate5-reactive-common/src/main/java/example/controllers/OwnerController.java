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

import example.controllers.dto.OwnerDto;
import example.domain.Owner;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.stage.Stage;

import javax.persistence.EntityManager;
import javax.validation.constraints.NotBlank;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

@Controller("/owners")
class OwnerController {

    private final Stage.SessionFactory sessionFactory;
    private final Mapper mapper;

    OwnerController(SessionFactory sessionFactory, Mapper mapper) {
        this.sessionFactory = sessionFactory.unwrap(Stage.SessionFactory.class);
        this.mapper = mapper;
    }

    @Get
    CompletionStage<List<OwnerDto>> all() {
        return sessionFactory.withSession(session -> session.createQuery("from Owner", Owner.class)
                .getResultList().thenApply(owners -> owners.stream()
                        .map(mapper::toOwnerDto)
                        .collect(Collectors.toList())));
    }

    @Get("/{name}")
    CompletionStage<OwnerDto> byName(@NotBlank String name) {
        return sessionFactory.withSession(session -> session.createQuery("from Owner where name = :name", Owner.class)
                .setParameter("name", name)
                .getResultList()
                .thenApply(owners -> owners.stream()
                        .findFirst()
                        .map(mapper::toOwnerDto)
                        .orElse(null)));
    }

}