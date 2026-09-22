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

import example.micronaut.mappers.GenreMapper;
import io.micronaut.configuration.mybatis.MyBatisMapperScanRegistration;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the mapper registration was generated at compile time instead of relying on runtime package scanning.
 */
@MicronautTest
class MapperScanRegistrationTest {

    @Inject
    Configuration configuration;

    @Test
    void registrationIsGeneratedAtCompileTime() {
        List<MyBatisMapperScanRegistration> registrations = new ArrayList<>();
        SoftServiceLoader.load(MyBatisMapperScanRegistration.class).collectAll(registrations);

        assertEquals(1, registrations.size());
        assertEquals("default", registrations.get(0).getDatasourceName());
        assertEquals("example.micronaut.CustomConfigurationCustomizer$MyBatisMapperScanRegistration",
            registrations.get(0).getClass().getName());
        assertTrue(configuration.hasMapper(GenreMapper.class));
        assertTrue(configuration.isMapUnderscoreToCamelCase());
    }
}
