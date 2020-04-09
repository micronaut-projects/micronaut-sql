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
package io.micronaut.configuration.jasync.health;

import com.github.jasync.sql.db.Connection;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.health.HealthStatus;
import io.micronaut.management.endpoint.health.HealthEndpoint;
import io.micronaut.management.health.indicator.HealthIndicator;
import io.micronaut.management.health.indicator.HealthResult;
import io.reactivex.Flowable;
import org.reactivestreams.Publisher;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A  {@link HealthIndicator} for reactive Postgres client.
 *
 * @author puneetbehl
 * @since 1.0
 */
@Requires(beans = HealthEndpoint.class)
@Requires(property = HealthEndpoint.PREFIX + ".jasync.enabled", notEquals = StringUtils.FALSE)
@Singleton
public class JasyncHealthIndicator implements HealthIndicator {

    public static final String NAME = "postgres-reactive";
    public static final String QUERY = "SELECT version();";
    private final Connection client;

    /**
     * Constructor.
     *
     * @param client A pool of connections.
     */
    public JasyncHealthIndicator(Connection client) {
        this.client = client;
    }

    @Override
    public Publisher<HealthResult> getResult() {
        return Flowable.fromFuture(client.sendQuery(QUERY))
                .map(queryResult -> {
                    Map<String, String> error = new HashMap<>(1);
                    error.put("version", String.valueOf(queryResult.getRows().get(0).get(0)));
                    return HealthResult.builder(NAME, HealthStatus.UP).details(error).build();
                })
                .onErrorReturn(error -> HealthResult.builder(NAME, HealthStatus.DOWN).exception(error).build());
    }

}
