/*
 * Copyright 2017-2025 original authors
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
package example.jdbc.hikari.sync;

import example.sync.AbstractApp;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "h2")
@Property(name = "datasources.default.pool-name", value = H2App.POOL_NAME)
@Property(name = "custom-ds-factory", value = "false")
public class H2App extends AbstractApp {

    static final String POOL_NAME = "H2Pool";

    @Test
    void verifyMetrics(MeterRegistry meterRegistry) {
        Gauge activeGauge = meterRegistry.find("hikaricp.connections.active").tag("pool", POOL_NAME).gauge();
        Double active = activeGauge != null ? activeGauge.value() : null;
        Gauge idleGauge = meterRegistry.find("hikaricp.connections.idle").tag("pool", POOL_NAME).gauge();
        Double idle = idleGauge != null ? idleGauge.value() : null;
        assertNotNull(active, "Active connection metric should be present");
        assertNotNull(idle, "Idle connection metric should be present");
        assertTrue(active >= 0, "Active connections must be non-negative");
        assertTrue(idle >= 0, "Idle connections must be non-negative");
    }
}

