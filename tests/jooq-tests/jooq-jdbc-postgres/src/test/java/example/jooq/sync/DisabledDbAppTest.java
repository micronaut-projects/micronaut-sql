/*
 * Copyright 2017-2024 original authors
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

import io.micronaut.context.annotation.Property;
import io.micronaut.context.exceptions.NoSuchBeanException;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "datasources.enabled", value = "false")
@Property(name = "datasources.default.db-type", value = "postgres")
@Property(name = "jooq.datasources.default.sql-dialect", value = "postgres")
class DisabledDbAppTest {

    @Test
    void serverRunning(EmbeddedServer embeddedServer) {
        assertTrue(embeddedServer.isRunning());
    }

    @Test
    void canTestNonDbBeans(EmbeddedServer embeddedServer) {
        Integer i = embeddedServer.getApplicationContext().getBean(ConversionService.class).convert("10", Integer.class).orElse(999);
        assertEquals(10, i);
    }

    @Test
    void noDatasourceDefined(EmbeddedServer embeddedServer) {
        assertThrows(NoSuchBeanException.class, () -> embeddedServer.getApplicationContext().getBean(DataSource.class));
    }
}

