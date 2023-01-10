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
package io.micronaut.configuration.hibernate6.jpa.proxy;

import io.micronaut.core.annotation.Internal;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.AbstractLazyInitializer;

/**
 * Basic {@link org.hibernate.proxy.LazyInitializer}.
 *
 * @author Denis Stepanov
 * @since 3.3.0
 */
@Internal
final class IntroducedHibernateProxyLazyInitializer extends AbstractLazyInitializer {

    protected final Class<?> persistentClass;

    protected IntroducedHibernateProxyLazyInitializer(String entityName,
                                                      Class<?> persistentClass,
                                                      Object id,
                                                      SharedSessionContractImplementor session) {
        super(entityName, id, session);
        this.persistentClass = persistentClass;
    }

    @Override
    public Class<?> getPersistentClass() {
        return persistentClass;
    }

}
