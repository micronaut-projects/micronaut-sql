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
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import jakarta.validation.ConstraintViolationException
import io.micronaut.test.annotation.Sql

@Sql(scripts = "classpath:schema.sql")
@Sql(scripts = "classpath:rollback.sql", phase = Sql.Phase.AFTER_ALL)
@MicronautTest
class GenreRepositoryImplSpec extends Specification {

    @Inject
    GenreRepository genreRepository

    void constraintsAreValidatedForFindAll() {
        when:
        genreRepository.findAll(null)

        then:
        thrown(ConstraintViolationException)
    }

    void constraintsAreValidatedForSave() {
        when:
        genreRepository.save(null)

        then:
        thrown(ConstraintViolationException)
    }

    void constraintsAreValidatedForUpdateNameIsNull() {
        when:
        genreRepository.update(12, null)

        then:
        thrown(ConstraintViolationException)
    }

    void constraintsAreValidatedForUpdateNameIsBlank() {
        when:
        genreRepository.update(4, '')

        then:
        thrown(ConstraintViolationException)
    }
}
