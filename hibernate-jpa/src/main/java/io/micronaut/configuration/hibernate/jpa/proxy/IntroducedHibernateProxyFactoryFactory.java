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
package io.micronaut.configuration.hibernate.jpa.proxy;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.spi.BasicProxyFactory;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.resource.beans.spi.ManagedBeanRegistry;

import java.util.Arrays;

/**
 * Implementation of Hibernate's {@link ProxyFactoryFactory} that creates {@link IntroducedHibernateProxyFactory} with {@link BeanContext}.
 *
 * @author Denis Stepanov
 * @since 3.3.0
 */
@Internal
final class IntroducedHibernateProxyFactoryFactory implements ProxyFactoryFactory {

    @Override
    public ProxyFactory buildProxyFactory(SessionFactoryImplementor sessionFactory) {
        BeanContext beanContext = sessionFactory.getServiceRegistry()
                .getService(ManagedBeanRegistry.class)
                .getBean(BeanContext.class)
                .getBeanInstance();
        return new IntroducedHibernateProxyFactory(beanContext);
    }

    @Override
    public BasicProxyFactory buildBasicProxyFactory(Class superClass, Class[] interfaces) {
        // Fallback?
        throw new HibernateException("Proxying of basic type " + superClass + " and interfaces " + Arrays.toString(interfaces) + " not supported. Micronaut only supports generating proxies for POJOs and non-basic types. Disable Micronaut compile-time proxies or remove basic type proxy to proceed.");
    }

}
