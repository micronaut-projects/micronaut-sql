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
package io.micronaut.configuration.hibernate.jpa.conf.serviceregistry.builder.configures.internal;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.serviceregistry.builder.configures.StandardServiceRegistryBuilderConfigurer;
import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.core.annotation.Internal;
import org.hibernate.boot.registry.StandardServiceInitiator;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import java.util.Map;

/**
 * Configure compile proxies.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Prototype
final class CompileProxyConfigurer implements StandardServiceRegistryBuilderConfigurer {

    private final BeanProvider<BytecodeProvider> bytecodeProviderBeanProvider;

    CompileProxyConfigurer(BeanProvider<BytecodeProvider> bytecodeProviderBeanProvider) {
        this.bytecodeProviderBeanProvider = bytecodeProviderBeanProvider;
    }

    @Override
    public void configure(JpaConfiguration jpaConfiguration, StandardServiceRegistryBuilder standardServiceRegistryBuilder) {
        if (jpaConfiguration.isCompileTimeHibernateProxies()) {
            // It would be enough to add `ProxyFactoryFactory` by providing `BytecodeProvider` we eliminate bytecode Enhancer
            standardServiceRegistryBuilder.addInitiator(new StandardServiceInitiator<BytecodeProvider>() {
                @Override
                public BytecodeProvider initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
                    return bytecodeProviderBeanProvider.get();
                }

                @Override
                public Class<BytecodeProvider> getServiceInitiated() {
                    return BytecodeProvider.class;
                }
            });
        }
    }

}
