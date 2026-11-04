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
package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.context.env.Environment;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

/**
 * Tells each {@link PoolTypeConfiguration} as it is created where to look for the connection factory class. It holds
 * the environment, so that the configuration beans the pool is made from do not receive it, which would keep
 * development mode from retaining the pool.
 *
 * @since 7.3.0
 */
@Internal
@Singleton
final class PoolTypeConfigurer implements BeanCreatedEventListener<PoolTypeConfiguration> {

    private final Environment environment;

    /**
     * @param environment The environment
     */
    PoolTypeConfigurer(Environment environment) {
        this.environment = environment;
    }

    @Override
    public PoolTypeConfiguration onCreated(BeanCreatedEvent<PoolTypeConfiguration> event) {
        PoolTypeConfiguration configuration = event.getBean();
        configuration.useEnvironmentSettings(environment.getClassLoader(), environment.isDevelopmentMode());
        return configuration;
    }
}
