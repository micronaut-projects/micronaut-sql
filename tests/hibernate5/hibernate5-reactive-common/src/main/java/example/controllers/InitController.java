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

import example.domain.Owner;
import example.domain.Pet;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletionStage;

@Controller("/init")
class InitController {
    private static final Logger LOG = LoggerFactory.getLogger(InitController.class);

    private final Stage.SessionFactory sessionFactory;

    public InitController(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory.unwrap(Stage.SessionFactory.class);
    }

    @Get
    CompletionStage<Void> init() {
        return sessionFactory.withTransaction(session -> {
            Owner fred = new Owner();
            fred.setName("Fred");
            fred.setAge(45);
            Owner barney = new Owner();
            barney.setName("Barney");
            barney.setAge(40);

            Pet dino = new Pet();
            dino.setName("Dino");
            dino.setOwner(fred);
            Pet bp = new Pet();
            bp.setName("Baby Puss");
            bp.setOwner(fred);
            bp.setType(Pet.PetType.CAT);
            Pet hoppy = new Pet();
            hoppy.setName("Hoppy");
            hoppy.setOwner(barney);
            return session.persist(fred, barney, dino, bp, hoppy);
        });
    }

}