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
package io.micronaut.configuration.hibernate6.jpa.conf.serviceregistry.builder.supplier;

import io.micronaut.configuration.hibernate6.jpa.JpaConfiguration;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.annotation.NonNull;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Creator of {@link StandardServiceRegistryBuilderCreator}.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Indexed(StandardServiceRegistryBuilderCreator.class)
public interface StandardServiceRegistryBuilderCreator {

    /**
     * Create {@link StandardServiceRegistryBuilderCreator} based on {@link JpaConfiguration}.
     *
     * @param jpaConfiguration The JPA configuration
     * @return new instance of {@link StandardServiceRegistryBuilderCreator}
     */
    @NonNull
    StandardServiceRegistryBuilder create(@NonNull JpaConfiguration jpaConfiguration);

}
