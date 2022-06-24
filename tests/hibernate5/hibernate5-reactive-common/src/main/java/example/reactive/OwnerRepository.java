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
package example.reactive;

import example.domain.IOwner;
import jakarta.inject.Singleton;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.stage.Stage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Singleton
class OwnerRepository implements IOwnerRepository {

    private final Stage.SessionFactory sessionFactory;

    OwnerRepository(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory.unwrap(Stage.SessionFactory.class);
    }

    @Override
    public IOwner create() {
        return new Owner();
    }

    @Override
    public Mono<Void> save(IOwner entity) {
        return withSessionMono(session -> Mono.fromCompletionStage(() -> session.persist(entity)));
    }

    @Override
    public Mono<? extends IOwner> findById(Long id) {
        return withSessionMono(session -> Mono.fromCompletionStage(() -> session.find(Owner.class, id)));
    }

    @Override
    public Mono<? extends IOwner> findByName(String name) {
        return withSessionMono(session -> Mono.fromCompletionStage(() -> session.createQuery("from Owner where name = :name", Owner.class)
                .setParameter("name", name)
                .getResultList())
            .map(results -> results.iterator().next()));
    }

    @Override
    public Flux<? extends IOwner> findAll() {
        return withSessionFlux(session -> Mono.fromCompletionStage(() -> session.createQuery("from Owner", Owner.class).getResultList())
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
