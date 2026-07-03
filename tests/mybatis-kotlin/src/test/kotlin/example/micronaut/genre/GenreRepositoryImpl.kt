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
import io.micronaut.core.annotation.NonNull
import jakarta.inject.Singleton
import java.util.Optional
import jakarta.validation.constraints.NotBlank

@Singleton // <1>
open class GenreRepositoryImpl(private val genreMapper: GenreMapper) : GenreRepository {

    override fun findById(id: Long): Optional<Genre> =
        Optional.ofNullable(genreMapper.findById(id))

    @NonNull
    override fun save(@NotBlank name: String): Genre {
        val genre = Genre(name)
        genreMapper.save(genre)
        return genre
    }

    override fun deleteById(id: Long) {
        findById(id).ifPresent { genreMapper.deleteById(id) }
    }

    @NonNull
    override fun findAll(args: ListingArguments): List<Genre> {
        if (args.max != null && args.sort != null && args.offset != null && args.order != null) {
            return genreMapper.findAllByOffsetAndMaxAndSortAndOrder(
                args.offset!!,
                args.max!!,
                args.sort!!,
                args.order!!)
        }

        if (args.max != null && args.offset != null) {
            return genreMapper.findAllByOffsetAndMax(args.offset!!, args.max!!)
        }

        if (args.sort != null && args.order != null) {
            return genreMapper.findAllBySortAndOrder(args.sort!!, args.order!!)
        }

        return genreMapper.findAll()
    }

    override fun update(id: Long, @NotBlank name: String): Int {
        genreMapper.update(id, name)
        return -1
    }
}
