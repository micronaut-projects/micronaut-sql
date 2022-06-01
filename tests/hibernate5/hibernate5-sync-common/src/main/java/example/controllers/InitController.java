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
import io.micronaut.transaction.annotation.TransactionalAdvice;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

@Controller("/init")
class InitController {
    private static final Logger LOG = LoggerFactory.getLogger(InitController.class);

    private final EntityManager entityManager;

    public InitController(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Get
    @TransactionalAdvice
    void init() {
        try {
            Class.forName("net.bytebuddy.ByteBuddy");
            throw new IllegalStateException("ByteBuddy shouldn't be present on classpath");
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        try {
            Class.forName("javassist.util.proxy.ProxyFactory");
            throw new IllegalStateException("Javassist shouldn't be present on classpath");
        } catch (ClassNotFoundException e) {
            // Ignore
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Running on " + System.getProperty("java.version") + " " + System.getProperty("java.vendor.version"));
            LOG.info("Populating data");
        }

        Owner fred = new Owner();
        fred.setName("Fred");
        fred.setAge(45);
        Owner barney = new Owner();
        barney.setName("Barney");
        barney.setAge(40);

        entityManager.persist(fred);
        entityManager.persist(barney);

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

        entityManager.persist(dino);
        entityManager.persist(bp);
        entityManager.persist(hoppy);
    }

}