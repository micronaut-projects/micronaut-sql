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
package example.micronaut.genre

import example.micronaut.ListingArguments
import example.micronaut.domain.Genre
import groovy.transform.CompileStatic
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

@CompileStatic
@Singleton // <1>
class GenreRepositoryImpl implements GenreRepository {

    private final GenreMapper genreMapper

    GenreRepositoryImpl(GenreMapper genreMapper) {
        this.genreMapper = genreMapper
    }

    @Override
    @NonNull
    Optional<Genre> findById(long id) {
        Optional.ofNullable(genreMapper.findById(id))
    }

    @Override
    @NonNull
    Genre save(@NonNull @NotBlank String name) {
        Genre genre = new Genre(name)
        genreMapper.save(genre)
        genre
    }

    @Override
    void deleteById(long id) {
        findById(id).ifPresent(genre -> genreMapper.deleteById(id))
    }

    @NonNull
    List<Genre> findAll(@NonNull @NotNull ListingArguments args) {

        if (args.max != null && args.sort != null && args.offset != null && args.order != null) {
            return genreMapper.findAllByOffsetAndMaxAndSortAndOrder(
                    args.offset,
                    args.max,
                    args.sort,
                    args.order)
        }

        if (args.max != null && args.offset!= null && (args.sort == null || args.order == null)) {
            return genreMapper.findAllByOffsetAndMax(args.offset, args.max)
        }

        if ((args.max == null || args.offset == null) && args.sort != null && args.order !=null) {
            return genreMapper.findAllBySortAndOrder(args.sort, args.order)
        }

        genreMapper.findAll()
    }

    @Override
    int update(long id, @NonNull @NotBlank String name) {
        genreMapper.update(id, name)
        -1
    }
}
