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
package io.micronaut.sql.hibernate.jpa.conf.sessionfactory.configure.internal;

import io.micronaut.sql.hibernate.jpa.JpaConfiguration;
import io.micronaut.sql.hibernate.jpa.conf.sessionfactory.configure.SessionFactoryBuilderConfigurer;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import org.hibernate.Interceptor;
import org.hibernate.boot.SessionFactoryBuilder;

/**
 * Interceptor configure.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Requires(bean = Interceptor.class)
final class InterceptorConfigurer implements SessionFactoryBuilderConfigurer {

    private final Interceptor interceptor;

    InterceptorConfigurer(Interceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void configure(JpaConfiguration jpaConfiguration, SessionFactoryBuilder sessionFactoryBuilder) {
        sessionFactoryBuilder.applyInterceptor(interceptor);
    }
}
