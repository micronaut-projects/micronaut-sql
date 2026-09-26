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
import io.micronaut.http.HttpHeaders
import io.micronaut.http.HttpRequest
import io.micronaut.http.HttpResponse
import io.micronaut.http.HttpStatus.CREATED
import io.micronaut.http.HttpStatus.NO_CONTENT
import io.micronaut.http.client.BlockingHttpClient
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.http.uri.UriBuilder
import io.micronaut.http.uri.UriTemplate
import io.micronaut.test.annotation.Sql
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.*

@Sql(scripts = ["classpath:schema.sql"])
@Sql(scripts = ["classpath:rollback.sql"], phase = Sql.Phase.AFTER_ALL)
@MicronautTest // <1>
class GenreControllerTest(@Client("/") val httpClient: HttpClient) { // <2>

    @Test
    fun supplyAnInvalidOrderTriggersValidationFailure() {
        assertThrows(HttpClientResponseException::class.java) {
            client.retrieve(
                    HttpRequest.GET<Any>("/genres/list?order=foo"),
                    Argument.of(List::class.java, Genre::class.java))
        }
    }

    @Test
    fun testFindNonExistingGenreReturns404() {
        assertThrows(HttpClientResponseException::class.java) {
            client.retrieve(HttpRequest.GET<Any>("/genres/99"), Argument.of(Genre::class.java))
        }
    }

    @Test
    fun testGenreCrudOperations() {
        val genreIds = mutableListOf<Long>()

        var response = saveGenre("DevOps")
        genreIds.add(entityId(response)!!)
        assertEquals(CREATED, response.status)

        response = saveGenre("Microservices") // <3>
        assertEquals(CREATED, response.status)

        val id = entityId(response)
        genreIds.add(id!!)
        var genre = show(id)
        assertEquals("Microservices", genre.name)

        response = update(id, "Micro-services")
        assertEquals(NO_CONTENT, response.status)

        genre = show(id)
        assertEquals("Micro-services", genre.name)

        var genres = listGenres(ListingArguments.builder().build())
        assertEquals(2, genres.size)

        genres = listGenres(ListingArguments.builder().max(1).build())
        assertEquals(1, genres.size)
        assertEquals("DevOps", genres[0].name)

        genres = listGenres(ListingArguments.builder().max(1).order("desc").sort("name").build())
        assertEquals(1, genres.size)
        assertEquals("Micro-services", genres[0].name)

        genres = listGenres(ListingArguments.builder().max(1).offset(10).build())
        assertEquals(0, genres.size)

        // cleanup:
        for (genreId in genreIds) {
            response = delete(genreId)
            assertEquals(NO_CONTENT, response.status)
        }
    }

    private fun listGenres(args: ListingArguments): List<Genre> {
        val uri = args.of(UriBuilder.of("/genres/list"))
        val request: HttpRequest<*> = HttpRequest.GET<Any>(uri)
        return client.retrieve(request, Argument.of(List::class.java, Genre::class.java)) as List<Genre> // <4>
    }

    private fun show(id: Long?): Genre {
        val uri = UriTemplate.of("/genres/{id}").expand(Collections.singletonMap<String, Any?>("id", id))
        val request: HttpRequest<*> = HttpRequest.GET<Any>(uri)
        return client.retrieve(request, Genre::class.java)
    }

    private fun update(id: Long?, name: String): HttpResponse<Any> {
        val request: HttpRequest<*> = HttpRequest.PUT("/genres", GenreUpdateCommand(id!!, name))
        return client.exchange(request) // <5>
    }

    private fun delete(id: Long): HttpResponse<Any> {
        val request: HttpRequest<*> = HttpRequest.DELETE<Any>("/genres/$id")
        return client.exchange(request)
    }

    private fun entityId(response: HttpResponse<Any>): Long? {
        val value = response.header(HttpHeaders.LOCATION) ?: return null
        val path = "/genres/"
        val index = value.indexOf(path)
        return if (index != -1) {
            java.lang.Long.valueOf(value.substring(index + path.length))
        } else null
    }

    private val client: BlockingHttpClient
        get() = httpClient.toBlocking()

    private fun saveGenre(genre: String): HttpResponse<Any> {
        val request: HttpRequest<*> = HttpRequest.POST("/genres", GenreSaveCommand(genre)) // <3>
        return client.exchange(request)
    }
}
