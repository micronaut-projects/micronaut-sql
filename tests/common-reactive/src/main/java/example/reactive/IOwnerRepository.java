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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.transaction.Transactional;

public interface IOwnerRepository {

    default Mono<Void> init() {
        return Mono.empty();
    }

    IOwner create();

    @Transactional(Transactional.TxType.MANDATORY)
    Mono<Void> save(IOwner entity);

    Mono<? extends IOwner> findById(Long id);

    Mono<? extends IOwner> findByName(String name);

    Flux<? extends IOwner> findAll();
}
