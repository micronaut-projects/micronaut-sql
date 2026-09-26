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
import example.micronaut.genre.GenreRepository;
import example.micronaut.genre.GenreSaveCommand;
import example.micronaut.genre.GenreUpdateCommand;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

@Controller("/genres") // <1>
public class GenreController {

    private final GenreRepository genreRepository;

    public GenreController(GenreRepository genreRepository) { // <2>
        this.genreRepository = genreRepository;
    }

    @Get("/{id}") // <3>
    public Genre show(long id) {
        return genreRepository
                .findById(id)
                .orElse(null); // <4>
    }

    @Put // <5>
    public HttpResponse<?> update(@Body @Valid GenreUpdateCommand command) { // <6>
        genreRepository.update(command.getId(), command.getName());
        return HttpResponse
                .noContent()
                .header(HttpHeaders.LOCATION, location(command.getId()).getPath()); // <7>
    }

    @Get(value = "/list{?args*}") // <8>
    public List<Genre> list(@Valid ListingArguments args) {
        return genreRepository.findAll(args);
    }

    @Post // <9>
    public HttpResponse<Genre> save(@Body @Valid GenreSaveCommand cmd) {
        Genre genre = genreRepository.save(cmd.getName());

        return HttpResponse
                .created(genre)
                .headers(headers -> headers.location(location(genre.getId())));
    }

    @Delete("/{id}") // <10>
    public HttpResponse<?> delete(long id) {
        genreRepository.deleteById(id);
        return HttpResponse.noContent();
    }

    private URI location(Long id) {
        return URI.create("/genres/" + id);
    }
}
