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
package io.micronaut.configuration.hibernate.jpa.conf.sessionfactory.configure;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.core.annotation.Indexed;
import io.micronaut.core.order.Ordered;
import org.hibernate.boot.SessionFactoryBuilder;

/**
 * Configure {@link SessionFactoryBuilder} using {@link JpaConfiguration}.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Indexed(SessionFactoryBuilderConfigurer.class)
public interface SessionFactoryBuilderConfigurer extends Ordered {

    /**
     * Configure {@link SessionFactoryBuilder}.
     *
     * @param jpaConfiguration      The JPA configuration
     * @param sessionFactoryBuilder The builder
     */
    void configure(JpaConfiguration jpaConfiguration, SessionFactoryBuilder sessionFactoryBuilder);

}
