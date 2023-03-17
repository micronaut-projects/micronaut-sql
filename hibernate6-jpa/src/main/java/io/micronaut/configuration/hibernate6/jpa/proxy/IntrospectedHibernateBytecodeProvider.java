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
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanProperty;
import jakarta.inject.Singleton;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.property.access.spi.PropertyAccess;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Compile-time proxies implementation of Hibernate's {@link BytecodeProvider}.
 * Implementation provides custom implementation of {@link ProxyFactoryFactory} and disables bytecode enhancer.
 *
 * @author Denis Stepanov
 * @since 3.3.0
 */
@Singleton
@Internal
public final class IntrospectedHibernateBytecodeProvider implements BytecodeProvider {

    private static final Enhancer NO_OP = (className, originalBytes) -> null;

    @Override
    public ProxyFactoryFactory getProxyFactoryFactory() {
        return new IntroducedHibernateProxyFactoryFactory();
    }

    @Override
    public ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames, Class[] types) {
        Optional<BeanIntrospection<?>> optionalBeanIntrospection = BeanIntrospector.SHARED.findIntrospection(clazz);
        return optionalBeanIntrospection.map(beanIntrospection -> new ReflectionOptimizer() {
            @Override
            public InstantiationOptimizer getInstantiationOptimizer() {
                return beanIntrospection::instantiate;
            }

            @Override
            public AccessOptimizer getAccessOptimizer() {
                BeanProperty[] beanProperties = beanIntrospection.getBeanProperties().toArray(new BeanProperty[0]);
                return new AccessOptimizer() {

                    private final String[] propertyNames = Arrays.stream(beanProperties)
                            .map(BeanProperty::getName)
                            .toArray(String[]::new);

                    @Override
                    public String[] getPropertyNames() {
                        return propertyNames;
                    }

                    @Override
                    public Object[] getPropertyValues(Object object) {
                        Object[] values = new Object[beanProperties.length];
                        for (int i = 0; i < beanProperties.length; i++) {
                            BeanProperty beanProperty = beanProperties[i];
                            values[i] = beanProperty.get(i);
                        }
                        return values;
                    }

                    @Override
                    public void setPropertyValues(Object object, Object[] values) {
                        for (int i = 0; i < beanProperties.length; i++) {
                            BeanProperty beanProperty = beanProperties[i];
                            beanProperty.set(object, values[i]);
                        }
                    }
                };
            }
        }).orElse(null);
    }

    @Override
    public ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap) {
        Optional<BeanIntrospection<?>> optionalBeanIntrospection = BeanIntrospector.SHARED.findIntrospection((Class) clazz);
        return optionalBeanIntrospection.map(beanIntrospection -> new ReflectionOptimizer() {
            @Override
            public InstantiationOptimizer getInstantiationOptimizer() {
                return beanIntrospection::instantiate;
            }

            @Override
            public AccessOptimizer getAccessOptimizer() {
                BeanProperty[] beanProperties = beanIntrospection.getBeanProperties().toArray(new BeanProperty[0]);
                return new AccessOptimizer() {

                    private final String[] propertyNames = Arrays.stream(beanProperties)
                        .map(BeanProperty::getName)
                        .toArray(String[]::new);

                    @Override
                    public String[] getPropertyNames() {
                        return propertyNames;
                    }

                    @Override
                    public Object[] getPropertyValues(Object object) {
                        Object[] values = new Object[beanProperties.length];
                        for (int i = 0; i < beanProperties.length; i++) {
                            BeanProperty beanProperty = beanProperties[i];
                            values[i] = beanProperty.get(object);
                        }
                        return values;
                    }

                    @Override
                    public void setPropertyValues(Object object, Object[] values) {
                        for (int i = 0; i < beanProperties.length; i++) {
                            BeanProperty beanProperty = beanProperties[i];
                            beanProperty.set(object, values[i]);
                        }
                    }
                };
            }
        }).orElse(null);
    }

    @Override
    public Enhancer getEnhancer(EnhancementContext enhancementContext) {
        return NO_OP;
    }

}
