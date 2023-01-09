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
package io.micronaut.configuration.hibernate.jpa.conf.sessionfactory.configure.internal;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.sessionfactory.configure.SessionFactoryBuilderConfigurer;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.hibernate.boot.SessionFactoryBuilder;

import javax.validation.ValidatorFactory;

/**
 * Configure of {@link ValidatorFactory}.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Requires(classes = ValidatorFactory.class, bean = ValidatorFactory.class)
@Prototype
final class JavaxValidatorFactoryConfigurer implements SessionFactoryBuilderConfigurer {

    private final ValidatorFactory validatorFactory;

    JavaxValidatorFactoryConfigurer(ValidatorFactory validatorFactory) {
        this.validatorFactory = validatorFactory;
    }

    @Override
    public void configure(JpaConfiguration jpaConfiguration, SessionFactoryBuilder sessionFactoryBuilder) {
        sessionFactoryBuilder.applyValidatorFactory(validatorFactory);
    }
}
