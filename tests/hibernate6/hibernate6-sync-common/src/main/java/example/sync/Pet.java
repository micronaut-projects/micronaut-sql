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
import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.serde.annotation.Serdeable;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

@Entity
@Serdeable
public class Pet implements IPet {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    private Owner owner;

    private PetType type = PetType.DOG;

    public Pet() {
    }

    @Creator
    public Pet(Long id, String name, Owner owner, @Nullable PetType type) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.type = type;
    }

    public PetType getType() {
        return type;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(IOwner owner) {
        this.owner = (Owner) owner;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setType(PetType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return asString();
    }
}
