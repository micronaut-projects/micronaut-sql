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

import example.domain.IOwner;
import example.domain.IPet;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller("/init")
class InitController {
    private static final Logger LOG = LoggerFactory.getLogger(InitController.class);

    private final IOwnerRepository ownerRepository;
    private final IPetRepository petRepository;

    InitController(IOwnerRepository ownerRepository, IPetRepository petRepository) {
        this.ownerRepository = ownerRepository;
        this.petRepository = petRepository;
    }

    @Get
    @Transactional
    void init() {
        IOwner fred = ownerRepository.create();
        fred.setName("Fred");
        fred.setAge(45);
        IOwner barney = ownerRepository.create();
        barney.setName("Barney");
        barney.setAge(40);

        ownerRepository.save(fred);
        ownerRepository.save(barney);

        IPet dino = petRepository.create();
        dino.setName("Dino");
        dino.setOwner(fred);
        IPet bp = petRepository.create();
        bp.setName("Baby Puss");
        bp.setOwner(fred);
        bp.setType(IPet.PetType.CAT);
        IPet hoppy = petRepository.create();
        hoppy.setName("Hoppy");
        hoppy.setOwner(barney);

        petRepository.save(dino);
        petRepository.save(bp);
        petRepository.save(hoppy);
    }

}
