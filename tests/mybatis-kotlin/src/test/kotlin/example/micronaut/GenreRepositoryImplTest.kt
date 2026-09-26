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

import example.micronaut.genre.GenreRepository
import io.micronaut.test.annotation.Sql
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import jakarta.validation.ConstraintViolationException
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

@Sql(scripts = ["classpath:schema.sql"])
@Sql(scripts = ["classpath:rollback.sql"], phase = Sql.Phase.AFTER_ALL)
@MicronautTest
class GenreRepositoryImplTest {

    @Inject
    lateinit var genreRepository: GenreRepository

    @Test
    fun constraintsAreValidatedForUpdateNameIsBlank() {
        assertThrows(ConstraintViolationException::class.java) { genreRepository.update(4, "") }
    }
}
