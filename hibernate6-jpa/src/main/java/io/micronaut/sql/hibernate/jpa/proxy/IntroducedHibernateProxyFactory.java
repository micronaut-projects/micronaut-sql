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
package io.micronaut.sql.hibernate.jpa.proxy;

import io.micronaut.context.BeanContext;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.proxy.LazyInitializer;
import org.hibernate.proxy.ProxyFactory;
import org.hibernate.type.CompositeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * Implementation of Hibernate {@link ProxyFactory}.
 * Compile time proxy will be used if one is added by annotating an entity with {@link GenerateProxy}.
 *
 * @author Denis Stepanov
 * @since 3.3.0
 */
@Internal
final class IntroducedHibernateProxyFactory implements ProxyFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntroducedHibernateProxyFactory.class);
    private static final Set<Class<?>> EXPECTED_INTERFACES = Collections.singleton(HibernateProxy.class);
    private static final String GET_HIBERNATE_LAZY_INITIALIZER = "getHibernateLazyInitializer";

    private final BeanContext beanContext;

    private String entityName;
    private Class<?> persistentClass;
    private CompositeType componentIdType;
    private Method getIdentifierMethod;
    private Method setIdentifierMethod;

    private BeanDefinition<?> beanDefinition;

    public IntroducedHibernateProxyFactory(BeanContext beanContext) {
        this.beanContext = beanContext;
    }

    @Override
    public void postInstantiate(String entityName,
                                Class<?> persistentClass,
                                Set<Class<?>> interfaces,
                                Method getIdentifierMethod,
                                Method setIdentifierMethod,
                                CompositeType componentIdType) throws HibernateException {
        this.getIdentifierMethod = getIdentifierMethod;
        this.setIdentifierMethod = setIdentifierMethod;
        this.entityName = entityName;
        this.persistentClass = persistentClass;
        this.componentIdType = componentIdType;
        if (LOGGER.isWarnEnabled() && !EXPECTED_INTERFACES.equals(interfaces)) {
            LOGGER.warn("Expected a single set of 'org.hibernate.proxy.HibernateProxy.class' got {}", interfaces);
        }
    }

    @Override
    public HibernateProxy getProxy(Object id, SharedSessionContractImplementor session) throws HibernateException {
        if (beanDefinition == null) {
            beanDefinition = beanContext.findBeanDefinition(persistentClass, null)
                    .orElseThrow(() -> new HibernateException("Cannot find a proxy class, please annotate " + persistentClass + " with @GenerateProxy."));
        }
        LazyInitializer lazyInitializer = new IntroducedHibernateProxyLazyInitializer(entityName, persistentClass, id, session);
        Object proxyTargetBean = beanContext.getBean(beanDefinition);
        IntroducedHibernateProxy introducedHibernateProxy = (IntroducedHibernateProxy) proxyTargetBean;
        introducedHibernateProxy.$registerInterceptor(context -> {
            String methodName = context.getMethodName();
            if ((Class<?>) context.getDeclaringType() == HibernateProxy.class) {
                if (GET_HIBERNATE_LAZY_INITIALIZER.equals(methodName)) {
                    return lazyInitializer;
                }
                // intercept HibernateProxy#writeReplace
                throw new HibernateException("InterceptedHibernateProxyFactory doesn't support serializing proxies");
            }

            // Handle identifier setter / getter

            Object[] parameterValues = context.getParameterValues();
            int params = parameterValues.length;
            if (params == 0 && getIdentifierMethod != null && methodName.equals(getIdentifierMethod.getName()) && lazyInitializer.isUninitialized()) {
                return lazyInitializer.getIdentifier();
            } else if (params == 1 && setIdentifierMethod != null && methodName.equals(setIdentifierMethod.getName())) {
                lazyInitializer.initialize();
                lazyInitializer.setIdentifier(parameterValues[0]);
            }

            // Equals/hashcode should work as other Hibernate proxy implementations:
            // methods present -> interceptor triggered: initializes proxy and delegate to the target
            // methods missing -> interceptor not triggered: proxy's methods are invoked

            ExecutableMethod<Object, Object> executableMethod = context.getExecutableMethod();
            if (componentIdType != null && componentIdType.isMethodOf(executableMethod.getTargetMethod())) {
                // An entity with multiple @Id's have identifier of the same entity type, an instance only with ids set.
                return executableMethod.invoke(lazyInitializer.getIdentifier(), parameterValues);
            }
            return executableMethod.invoke(lazyInitializer.getImplementation(), parameterValues);
        });
        return introducedHibernateProxy;
    }

}
