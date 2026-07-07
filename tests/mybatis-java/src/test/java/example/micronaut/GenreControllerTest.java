/*
 * Copyright 2017-2026 original authors
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
package example.micronaut;

import example.micronaut.domain.Genre;
import example.micronaut.genre.GenreSaveCommand;
import example.micronaut.genre.GenreUpdateCommand;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.http.uri.UriTemplate;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.micronaut.http.HttpHeaders.LOCATION;
import static io.micronaut.http.HttpStatus.CREATED;
import static io.micronaut.http.HttpStatus.NO_CONTENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import io.micronaut.test.annotation.Sql;

@Sql(scripts = "classpath:schema.sql")
@Sql(scripts = "classpath:rollback.sql", phase = Sql.Phase.AFTER_ALL)
@MicronautTest // <1>
class GenreControllerTest {

    @Inject
    @Client("/")
    HttpClient httpClient; // <2>

    @Test
    void supplyAnInvalidOrderTriggersValidationFailure() {
        assertThrows(HttpClientResponseException.class, () ->
                getClient().retrieve(
                        HttpRequest.GET("/genres/list?order=foo"),
                        Argument.of(List.class, Genre.class)));
    }

    @Test
    void testFindNonExistingGenreReturns404() {
        assertThrows(HttpClientResponseException.class, () ->
                getClient().retrieve(HttpRequest.GET("/genres/99"), Argument.of(Genre.class)));
    }

    @Test
    void testGenreCrudOperations() {
        List<Long> genreIds = new ArrayList<>();
        HttpResponse<?> response = saveGenre("DevOps");
        genreIds.add(entityId(response));
        assertEquals(CREATED, response.getStatus());

        response = saveGenre("Microservices"); // <3>
        assertEquals(CREATED, response.getStatus());

        Long id = entityId(response);
        genreIds.add(id);
        Genre genre = show(id);
        assertEquals("Microservices", genre.getName());

        response = update(id, "Micro-services");
        assertEquals(NO_CONTENT, response.getStatus());

        genre = show(id);
        assertEquals("Micro-services", genre.getName());

        List<Genre> genres = listGenres(ListingArguments.builder().build());
        assertEquals(2, genres.size());

        genres = listGenres(ListingArguments.builder().max(1).build());
        assertEquals(1, genres.size());
        assertEquals("DevOps", genres.get(0).getName());

        genres = listGenres(ListingArguments.builder().max(1).order("desc").sort("name").build());
        assertEquals(1, genres.size());
        assertEquals("Micro-services", genres.get(0).getName());

        genres = listGenres(ListingArguments.builder().max(1).offset(10).build());
        assertEquals(0, genres.size());

        // cleanup:
        for (long genreId : genreIds) {
            response = delete(genreId);
            assertEquals(NO_CONTENT, response.getStatus());
        }
    }

    private List<Genre> listGenres(ListingArguments args) {
        URI uri = args.of(UriBuilder.of("/genres/list"));
        HttpRequest<?> request = HttpRequest.GET(uri);
        return getClient().retrieve(request, Argument.of(List.class, Genre.class)); // <4>
    }

    private Genre show(Long id) {
        String uri = UriTemplate.of("/genres/{id}").expand(Collections.singletonMap("id", id));
        HttpRequest<?> request = HttpRequest.GET(uri);
        return getClient().retrieve(request, Genre.class);
    }

    private HttpResponse<?> update(Long id, String name) {
        HttpRequest<?> request = HttpRequest.PUT("/genres", new GenreUpdateCommand(id, name));
        return getClient().exchange(request); // <5>
    }

    private HttpResponse<?> delete(Long id) {
        HttpRequest<?> request = HttpRequest.DELETE("/genres/" + id);
        return getClient().exchange(request);
    }

    private Long entityId(HttpResponse<?> response) {
        String value = response.header(LOCATION);
        if (value == null) {
            return null;
        }

        String path = "/genres/";
        int index = value.indexOf(path);
        if (index != -1) {
            return Long.valueOf(value.substring(index + path.length()));
        }

        return null;
    }

    private BlockingHttpClient getClient() {
        return httpClient.toBlocking();
    }

    private HttpResponse<?> saveGenre(String genre) {
        HttpRequest<?> request = HttpRequest.POST("/genres", new GenreSaveCommand(genre)); // <3>
        return getClient().exchange(request);
    }
}
