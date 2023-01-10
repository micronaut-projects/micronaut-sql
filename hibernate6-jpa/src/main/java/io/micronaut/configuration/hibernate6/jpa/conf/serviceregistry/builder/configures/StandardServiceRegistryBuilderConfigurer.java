/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.configuration.hibernate6.jpa.conf.serviceregistry.builder.configures;

import io.micronaut.configuration.hibernate6.jpa.JpaConfiguration;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.NonNull;
import io.micronaut.core.order.Ordered;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Configure {@link StandardServiceRegistryBuilder} using {@link JpaConfiguration}.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Indexed(StandardServiceRegistryBuilderConfigurer.class)
public interface StandardServiceRegistryBuilderConfigurer extends Ordered {

    /**
     * Configure {@link StandardServiceRegistryBuilder}.
     *
     * @param jpaConfiguration               The JPA configuration
     * @param standardServiceRegistryBuilder The builder
     */
    void configure(@NonNull JpaConfiguration jpaConfiguration, @NonNull StandardServiceRegistryBuilder standardServiceRegistryBuilder);

}
