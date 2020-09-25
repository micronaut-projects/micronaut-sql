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
import example.controllers.dto.PetDto;
import example.domain.Owner;
import example.domain.Pet;

import javax.inject.Singleton;

@Singleton
public class Mapper {

    public PetDto toPetDto(Pet pet) {
        PetDto petDto = new PetDto();
        petDto.setId(pet.getId());
        petDto.setName(pet.getName());
        petDto.setType(pet.getType());
        petDto.setOwner(toOwnerDto(pet.getOwner()));
        return petDto;
    }

    public OwnerDto toOwnerDto(Owner owner) {
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setAge(owner.getAge());
        ownerDto.setId(owner.getId());
        ownerDto.setName(owner.getName());
        return ownerDto;
    }

}
