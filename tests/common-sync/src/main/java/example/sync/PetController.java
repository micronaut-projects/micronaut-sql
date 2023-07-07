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

import example.dto.PetDto;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Controller("/pets")
class PetController {

    private final IPetRepository petRepository;
    private final Mapper mapper;

    PetController(IPetRepository petRepository, Mapper mapper) {
        this.petRepository = petRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    @Get
    List<PetDto> all() {
        return petRepository.findAll()
                .stream()
                .map(mapper::toPetDto)
                .toList();
    }

    @Transactional(readOnly = true)
    @Get("/{name}")
    Optional<PetDto> byName(String name) {
        return petRepository.findByName(name).map(mapper::toPetDto);
    }

}
