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

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.transaction.annotation.TransactionalAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/destroy")
class DestroyController {

    private final IOwnerRepository ownerRepository;
    private final IPetRepository petRepository;

    DestroyController(IOwnerRepository ownerRepository, IPetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
    }

    @Get
    @TransactionalAdvice
    void destroy() {
        petRepository.findAll().forEach(it -> petRepository.delete(it));
        ownerRepository.findAll().forEach(it -> ownerRepository.delete(it));
    }

}
