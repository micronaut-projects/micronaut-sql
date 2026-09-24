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

import example.micronaut.mappers.GenreMapper
import io.micronaut.configuration.mybatis.MyBatisMapperScanRegistration
import io.micronaut.core.io.service.SoftServiceLoader
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.apache.ibatis.session.Configuration
import spock.lang.Specification

/**
 * Verifies that the mapper registration was generated at compile time instead of relying on runtime package scanning.
 */
@MicronautTest
class MapperScanRegistrationSpec extends Specification {

    @Inject
    Configuration configuration

    void "registration is generated at compile time"() {
        given:
        List<MyBatisMapperScanRegistration> registrations = []
        SoftServiceLoader.load(MyBatisMapperScanRegistration).collectAll(registrations)

        expect:
        registrations.size() == 1
        registrations[0].datasourceName == "default"
        registrations[0].class.name == 'example.micronaut.mappers.example_pmicronaut_pCustomConfigurationCustomizer$MyBatisMapperScanRegistration'
        configuration.hasMapper(GenreMapper)
        configuration.mapUnderscoreToCamelCase
    }
}
