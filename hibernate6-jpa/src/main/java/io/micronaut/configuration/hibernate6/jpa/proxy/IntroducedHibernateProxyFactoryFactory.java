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

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;
import org.hibernate.type.CompositeType;

import java.lang.reflect.Method;
import java.util.Set;

/**
 * Implementation of Hibernate's {@link ProxyFactoryFactory} that creates {@link IntroducedHibernateProxyFactory} with {@link BeanContext}.
 *
 * @author Denis Stepanov
 * @since 3.3.0
 */
@Internal
final class IntroducedHibernateProxyFactoryFactory implements ProxyFactoryFactory {

    private static final ProxyFactory NO_PROXY_FACTORY = new ProxyFactory() {
        @Override
        public void postInstantiate(String entityName, Class<?> persistentClass, Set<Class<?>> interfaces, Method getIdentifierMethod, Method setIdentifierMethod, CompositeType componentIdType) throws HibernateException {
            // no-op
        }

        @Override
        public HibernateProxy getProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
            throw new HibernateException("Generation of HibernateProxy instances at runtime is not allowed when the configured BytecodeProvider is 'none'; your model requires a more advanced BytecodeProvider to be enabled.");
        }
    };

    @Override
    public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
        ManagedBeanRegistry beanRegistry = sessionFactory.getServiceRegistry()
                .getService(ManagedBeanRegistry.class);
        BeanContext beanContext;
        try {
            beanContext = beanRegistry
                    .getBean(BeanContext.class)
                    .getBeanInstance();
            return new IntroducedHibernateProxyFactory(beanContext);
        } catch (org.hibernate.InstantiationException e) {
            return NO_PROXY_FACTORY;
        }
    }

    @Override
    public BasicProxyFactory buildBasicProxyFactory(Class superClassOrInterface) {
        return () -> {
            throw new HibernateException("NoneBasicProxyFactory is unable to generate a BasicProxy for type " + superClassOrInterface + ". Enable a different BytecodeProvider.");
        };
    }

}
