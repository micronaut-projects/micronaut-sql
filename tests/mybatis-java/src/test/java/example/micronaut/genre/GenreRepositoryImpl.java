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
package example.micronaut.genre;

import example.micronaut.ListingArguments;
import example.micronaut.domain.Genre;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.validation.Validated;
import jakarta.inject.Singleton;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Singleton // <1>
@Validated
public class GenreRepositoryImpl implements GenreRepository {

    private final GenreMapper genreMapper;

    public GenreRepositoryImpl(GenreMapper genreMapper) {
        this.genreMapper = genreMapper;
    }

    @Override
    @NonNull
    public Optional<Genre> findById(long id) {
        return Optional.ofNullable(genreMapper.findById(id));
    }

    @Override
    @NonNull
    public Genre save(@NonNull @NotBlank String name) {
        Genre genre = new Genre(name);
        genreMapper.save(genre);
        return genre;
    }

    @Override
    public void deleteById(long id) {
        findById(id).ifPresent(genre -> genreMapper.deleteById(id));
    }

    @Override
    @NonNull
    public List<Genre> findAll(@NonNull @NotNull ListingArguments args) {

        if (args.getMax() != null && args.getSort() != null && args.getOffset() != null && args.getOrder() != null) {
            return genreMapper.findAllByOffsetAndMaxAndSortAndOrder(
                    args.getOffset(),
                    args.getMax(),
                    args.getSort(),
                    args.getOrder());
        }

        if (args.getMax() != null && args.getOffset()!= null && (args.getSort() == null || args.getOrder() == null)) {
            return genreMapper.findAllByOffsetAndMax(args.getOffset(), args.getMax());
        }

        if ((args.getMax() == null || args.getOffset() == null) && args.getSort() != null && args.getOrder() !=null) {
            return genreMapper.findAllBySortAndOrder(args.getSort(), args.getOrder());
        }

        return genreMapper.findAll();
    }

    @Override
    public int update(long id, @NonNull @NotBlank String name) {
        genreMapper.update(id, name);
        return -1;
    }
}
