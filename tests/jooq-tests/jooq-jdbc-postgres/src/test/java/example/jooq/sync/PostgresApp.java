/*
 * Copyright 2017-2020 original authors
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
package example.jooq.sync;

import example.sync.AbstractApp;
import io.micronaut.configuration.jooq.JsonConverterProvider;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.jooq.Converter;
import org.jooq.JSON;
import org.jooq.JSONB;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "postgres")
@Property(name = "jooq.datasources.default.sql-dialect", value = "postgres")
@Property(name = "jooq.datasources.default.json-converter-enabled", value = "true")
public class PostgresApp extends AbstractApp {

    @Inject
    private JsonConverterProvider jsonConverterProvider;

    @Test
    void jsonConverterProviderConvertsJsonAndJsonb() {
        Owner owner = new Owner(1L, "Fred", 45);

        Converter<JSON, Owner> jsonToOwner = jsonConverterProvider.provide(JSON.class, Owner.class);
        Converter<JSONB, Owner> jsonbToOwner = jsonConverterProvider.provide(JSONB.class, Owner.class);

        assertNotNull(jsonToOwner);
        assertNotNull(jsonbToOwner);

        assertOwner(owner, jsonToOwner.from(JSON.valueOf("{\"id\":1,\"name\":\"Fred\",\"age\":45}")));
        assertOwner(owner, jsonbToOwner.from(JSONB.valueOf("{\"id\":1,\"name\":\"Fred\",\"age\":45}")));
        assertOwner(owner, jsonToOwner.from(jsonToOwner.to(owner)));
        assertOwner(owner, jsonbToOwner.from(jsonbToOwner.to(owner)));
    }

    private static void assertOwner(Owner expected, Owner actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getAge(), actual.getAge());
    }
}
