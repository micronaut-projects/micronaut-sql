/*
 * Copyright 2017-2023 original authors
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
package io.micronaut.configuration.hibernate.jpa.proxy;

import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.property.access.spi.PropertyAccess;

import java.util.Map;

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

    private static final Enhancer NO_OP = new Enhancer() {

        @Override
        public byte[] enhance(String className, byte[] originalBytes) throws EnhancementException {
            return null;
        }

        @Override
        public void discoverTypes(String className, byte[] originalBytes) throws EnhancementException {
            // Does nothing
        }
    };

    @Override
    public ProxyFactoryFactory getProxyFactoryFactory() {
        return new IntroducedHibernateProxyFactoryFactory();
    }

    @Override
    public ReflectionOptimizer getReflectionOptimizer(Class clazz, String[] getterNames, String[] setterNames, Class[] types) {
        // This is deprecated and no longer used
        return null;
    }

    @Override
    public ReflectionOptimizer getReflectionOptimizer(Class<?> clazz, Map<String, PropertyAccess> propertyAccessMap) {
        // Prev implementation doesn't return accurate optimizer and bean properties
        // So some tests are failing. Returning null fixes some failing tests for now
        return null;
    }

    @Override
    public Enhancer getEnhancer(EnhancementContext enhancementContext) {
        return NO_OP;
    }

}
