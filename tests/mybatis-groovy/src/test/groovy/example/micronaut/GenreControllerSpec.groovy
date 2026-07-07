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
package example.micronaut

import example.micronaut.domain.Genre
import example.micronaut.genre.GenreSaveCommand
import example.micronaut.genre.GenreUpdateCommand
import io.micronaut.core.type.Argument
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.http.uri.UriTemplate
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static io.micronaut.http.HttpHeaders.LOCATION
import static io.micronaut.http.HttpStatus.CREATED
import static io.micronaut.http.HttpStatus.NO_CONTENT
import io.micronaut.test.annotation.Sql

@Sql(scripts = "classpath:schema.sql")
@Sql(scripts = "classpath:rollback.sql", phase = Sql.Phase.AFTER_ALL)
@MicronautTest // <1>
class GenreControllerSpec extends Specification {

    @Inject
    @Client('/')
    HttpClient httpClient // <2>

    void supplyAnInvalidOrderTriggersValidationFailure() {
        when:
        client.retrieve(
                HttpRequest.GET('/genres/list?order=foo'),
                Argument.of(List, Genre))

        then:
        thrown(HttpClientResponseException)
    }

    void testFindNonExistingGenreReturns404() {
        when:
        client.retrieve(HttpRequest.GET('/genres/99'), Argument.of(Genre))

        then:
        thrown(HttpClientResponseException)
    }

    void testGenreCrudOperations() {
        given:
        List<Long> genreIds = []

        when:
        HttpResponse<?> response = saveGenre('DevOps')
        genreIds << entityId(response)

        then:
        CREATED == response.status

        when:
        response = saveGenre('Microservices') // <3>

        then:
        CREATED == response.status

        when:
        Long id = entityId(response)
        genreIds.add(id)
        Genre genre = show(id)

        then:
        'Microservices' == genre.name

        when:
        response = update(id, 'Micro-services')

        then:
        NO_CONTENT == response.status

        when:
        genre = show(id)

        then:
        'Micro-services' == genre.name

        when:
        List<Genre> genres = listGenres(ListingArguments.builder().build())

        then:
        2 == genres.size()

        when:
        genres = listGenres(ListingArguments.builder().max(1).build())

        then:
        1 == genres.size()
        'DevOps' == genres[0].name

        when:
        genres = listGenres(ListingArguments.builder().max(1).order('desc').sort('name').build())

        then:
        1 == genres.size()
        'Micro-services' == genres[0].name

        when:
        genres = listGenres(ListingArguments.builder().max(1).offset(10).build())

        then:
        0 == genres.size()

        cleanup:
        for (long genreId : genreIds) {
            response = delete(genreId)
            assert NO_CONTENT == response.status
        }
    }

    private List<Genre> listGenres(ListingArguments args) {
        URI uri = args.of(UriBuilder.of('/genres/list'))
        HttpRequest<?> request = HttpRequest.GET(uri)
        client.retrieve(request, Argument.of(List, Genre)) // <4>
    }

    private Genre show(Long id) {
        String uri = UriTemplate.of('/genres/{id}').expand(id: id)
        HttpRequest<?> request = HttpRequest.GET(uri)
        client.retrieve(request, Genre)
    }

    private HttpResponse<?> update(Long id, String name) {
        HttpRequest<?> request = HttpRequest.PUT('/genres', new GenreUpdateCommand(id, name))
        client.exchange(request) // <5>
    }

    private HttpResponse<?> delete(Long id) {
        HttpRequest<?> request = HttpRequest.DELETE('/genres/' + id)
        client.exchange(request)
    }

    private Long entityId(HttpResponse<?> response) {
        String value = response.header(LOCATION)
        if (value == null) {
            return null
        }

        String path = '/genres/'
        int index = value.indexOf(path)
        if (index != -1) {
            return value.substring(index + path.length()) as long
        }

        null
    }

    private BlockingHttpClient getClient() {
        httpClient.toBlocking()
    }

    private HttpResponse<?> saveGenre(String genre) {
        HttpRequest<?> request = HttpRequest.POST('/genres', new GenreSaveCommand(genre)) // <3>
        client.exchange(request)
    }
}
