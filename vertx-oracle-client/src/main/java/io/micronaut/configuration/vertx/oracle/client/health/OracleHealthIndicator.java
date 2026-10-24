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
package io.micronaut.configuration.vertx.oracle.client.health;

import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

import java.util.Collections;

/**
 * A {@link HealthIndicator} for the Vert.x Oracle client.
 *
 * @since 7.3.0
 */
@Requires(beans = HealthEndpoint.class)
@Requires(property = HealthEndpoint.PREFIX + ".vertx.oracle.client.enabled", notEquals = StringUtils.FALSE)
@Singleton
public class OracleHealthIndicator implements HealthIndicator {
    public static final String NAME = "vertx-oracle-client";
    public static final String QUERY = "SELECT version_full FROM product_component_version WHERE product LIKE 'Oracle%'";
    private final Pool client;

    /**
     * @param client A pool of connections.
     */
    public OracleHealthIndicator(Pool client) {
        this.client = client;
    }

    @Override
    public Publisher<HealthResult> getResult() {
        return Mono.fromCompletionStage(
            client.query(QUERY).execute()
                .toCompletionStage()
                .thenApply(rows -> {
                    HealthResult.Builder status = HealthResult.builder(NAME, HealthStatus.UP);
                    Row row = rows.iterator().next();
                    status.details(Collections.singletonMap("version", row.getString(0)));
                    return status.build();
                })
                .exceptionally(this::buildErrorResult)
        );
    }

    private HealthResult buildErrorResult(Throwable throwable) {
        return HealthResult.builder(NAME, HealthStatus.DOWN).exception(throwable).build();
    }
}
