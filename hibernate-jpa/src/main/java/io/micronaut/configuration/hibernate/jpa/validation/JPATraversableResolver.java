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
package io.micronaut.configuration.hibernate.jpa.validation;

import jakarta.inject.Singleton;
import jakarta.validation.Path;
import jakarta.validation.TraversableResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Persistence;
import java.lang.annotation.ElementType;

/**
 * An implementation of {@code TraversableResolver} which is aware of JPA 2 and utilizes {@code PersistenceUtil} to
 * query the reachability of a property.
 * This resolver will be automatically enabled if JPA 2 is on the classpath and the default {@code TraversableResolver} is
 * used.
 * <p>
 * This class needs to be public as it's instantiated via a privileged action that is not in this package.
 *
 * @author Hardy Ferentschik
 * @author Emmanuel Bernard
 */
@Singleton
public class JPATraversableResolver implements TraversableResolver {

    private static final Logger LOG = LoggerFactory.getLogger(JPATraversableResolver.class);

    @Override
    public final boolean isReachable(Object traversableObject,
                                     Path.Node traversableProperty,
                                     Class<?> rootBeanType,
                                     Path pathToTraversableObject,
                                     ElementType elementType) {
        if (LOG.isTraceEnabled()) {
            LOG.trace(
                    "Calling isReachable on object {} with node name {}.",
                    traversableObject,
                    traversableProperty.getName()
            );
        }

        if (traversableObject == null) {
            return true;
        }

        return Persistence.getPersistenceUtil().isLoaded(traversableObject, traversableProperty.getName());
    }

    @Override
    public final boolean isCascadable(Object traversableObject, Path.Node traversableProperty, Class<?> rootBeanType, Path pathToTraversableObject, ElementType elementType) {
        return true;
    }
}
