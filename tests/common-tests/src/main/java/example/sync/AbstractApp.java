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

import example.dto.OwnerDto;
import example.dto.PetDto;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.support.TestPropertyProvider;
import io.micronaut.testresources.client.TestResourcesClient;
import io.micronaut.testresources.client.TestResourcesClientFactory;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractApp implements TestPropertyProvider {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    ApplicationContext context;

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("micronaut.test.resources.scope", getClass().getName());
        return properties;
    }

    @AfterAll
    public void cleanup() {
        try {
            TestResourcesClient testResourcesClient = TestResourcesClientFactory.extractFrom(context);
            testResourcesClient.closeScope(getClass().getName());
        } catch (Exception e) {
            // ignore
        }
    }

    @Test
    @Order(1)
    void shouldInit() {
        HttpResponse<Void> response = client.toBlocking().exchange(HttpRequest.GET("/init"));
        assertEquals(HttpStatus.OK, response.getStatus());
    }

    @Test
    @Order(2)
    void shouldFetchOwners() {
        List<OwnerDto> results = client.toBlocking().retrieve(HttpRequest.GET("/owners"), Argument.listOf(OwnerDto.class));
        assertEquals(2, results.size());
        assertEquals("Fred", results.get(0).getName());
        assertEquals("Barney", results.get(1).getName());
    }

    @Test
    @Order(3)
    void shouldFetchOwnerByName() {
        Map result = client.toBlocking().retrieve(HttpRequest.GET("/owners/Fred"), Map.class);
        assertEquals("Fred", result.get("name"));
    }

    @Test
    @Order(4)
    @Disabled("Getting Flux (findAll) is failing using hibernate-reactive 2.0")
    void shouldFetchPets() {
        List<PetDto> results = client.toBlocking().retrieve(HttpRequest.GET("/pets"), Argument.listOf(PetDto.class));
        assertEquals(3, results.size());
        assertEquals("Dino", results.get(0).getName());
        assertEquals("Fred", results.get(0).getOwner().getName());
        assertEquals("Baby Puss", results.get(1).getName());
        assertEquals("Fred", results.get(1).getOwner().getName());
        assertEquals("Hoppy", results.get(2).getName());
        assertEquals("Barney", results.get(2).getOwner().getName());
    }

    @Test
    @Order(5)
    void shouldFetchPetByName() {
        Map result = client.toBlocking().retrieve(HttpRequest.GET("/pets/Dino"), Map.class);
        assertEquals("Dino", result.get("name"));
        assertEquals("Fred", ((Map) result.get("owner")).get("name"));
    }

    @Test
    @Order(6)
    @Disabled("Getting Flux (findAll) is failing using hibernate-reactive 2.0")
    void shouldFetchPetsParallel() {
        List<List<PetDto>> resultsList = Flux.range(1, 1000)
            .flatMap(it -> client.retrieve(HttpRequest.GET("/pets"), Argument.listOf(PetDto.class)))
            .collectList()
            .block();

        resultsList.forEach(it -> {
            assertEquals(3, it.size());
            assertEquals("Dino", it.get(0).getName());
            assertEquals("Fred", it.get(0).getOwner().getName());
            assertEquals("Baby Puss", it.get(1).getName());
            assertEquals("Fred", it.get(1).getOwner().getName());
            assertEquals("Hoppy", it.get(2).getName());
            assertEquals("Barney", it.get(2).getOwner().getName());
        });
    }

    @Test
    @Order(7)
    void shouldFetchPetByNameParallel() {
        List<Map> resultsList = Flux.range(1, 1000)
            .flatMap(it -> client.retrieve(HttpRequest.GET("/pets/Dino"), Map.class))
            .collectList()
            .block();

        resultsList.forEach(it -> assertEquals("Dino", it.get("name")));
    }

    @Test
    @Order(8)
    @Disabled("Getting Flux (findAll) is failing using hibernate-reactive 2.0")
    void shouldDestroy() {
        HttpResponse<Void> response = client.toBlocking().exchange(HttpRequest.GET("/destroy"));
        assertEquals(HttpStatus.OK, response.getStatus());
    }
}
