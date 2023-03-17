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
package example.hibernate6.reactive;

import example.domain.IPet;
import example.reactive.IPetRepository;
import jakarta.inject.Singleton;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.stage.Stage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Singleton
class PetRepository implements IPetRepository {

    private final Stage.SessionFactory sessionFactory;

    PetRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory.unwrap(Stage.SessionFactory.class);
    }

    @Override
    public Mono<Void> destroy() {
        return findAll().flatMap(it -> delete(it)).then();
    }

    @Override
    public IPet create() {
        return new Pet();
    }

    @Override
    public Mono<Void> save(IPet entity) {
        return withSessionMono(session -> Mono.fromCompletionStage(() -> session.persist(entity)));
    }

    @Override
    public Mono<Void> delete(IPet entity) {
        return withSessionMono(session -> Mono.fromCompletionStage(() -> session.remove(entity)));
    }

    @Override
    public Mono<? extends Pet> findByName(String name) {
        return withSessionMono(session -> Mono.fromCompletionStage(() -> session.createQuery("from Pet where name = :name", Pet.class)
                .setParameter("name", name)
                .getResultList())
            .map(results -> results.iterator().next())
            .flatMap(pet -> {
                if (Hibernate.isInitialized(pet.getOwner())) {
                    throw new IllegalStateException("Expected not initialized owner ref");
                }
                // HR000037: Reactive sessions do not support transparent lazy fetching
                // - use Session.fetch() (entity 'example.domain.Owner' with id '1' was not loaded)
                return Mono.fromCompletionStage(() -> session.fetch(pet.getOwner())).map(owner -> {
                    pet.setOwner(owner);
                    if (!Hibernate.isInitialized(pet.getOwner())) {
                        throw new IllegalStateException("Expected initialized owner ref");
                    }
                    return pet;
                });
            }));
    }

    @Override
    public Flux<? extends Pet> findAll() {
        return withSessionFlux(session -> Mono.fromCompletionStage(() -> session.createQuery("from Pet", Pet.class)
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
                }))
            .flatMapMany(Flux::fromIterable));
    }

    private <T> Mono<T> withSessionMono(Function<Stage.Session, Mono<T>> consumer) {
        return Mono.deferContextual(contextView -> {
            Stage.Session session = contextView.getOrDefault(TxManager.SESSION_KEY, null);
            if (session == null) {
                return Mono.fromCompletionStage(() -> sessionFactory.withSession(s -> consumer.apply(s).toFuture()));
            }
            return consumer.apply(session);
        });
    }

    private <T> Flux<T> withSessionFlux(Function<Stage.Session, Flux<T>> consumer) {
        return Flux.deferContextual(contextView -> {
            Stage.Session session = contextView.getOrDefault(TxManager.SESSION_KEY, null);
            if (session == null) {
                return Mono.fromCompletionStage(() -> sessionFactory.withSession(s -> consumer.apply(s).collectList().toFuture()))
                    .flatMapMany(Flux::fromIterable);
            }
            return consumer.apply(session);
        });
    }
}
