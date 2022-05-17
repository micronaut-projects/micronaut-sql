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
package io.micronaut.configuration.hibernate.reactive.conf;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.serviceregistry.builder.supplier.StandardServiceRegistryBuilderCreator;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.TypeHint;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.reactive.persister.entity.impl.ReactiveSingleTableEntityPersister;
import org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder;

import java.util.ServiceLoader;

@TypeHint(value = ServiceLoader.Provider.class, accessType = {TypeHint.AccessType.ALL_PUBLIC})
final class Provider {
}

@TypeHint({
        org.hibernate.reactive.logging.impl.Log_$logger.class,
        ReactiveServiceRegistryBuilder.class,
        ReactiveSingleTableEntityPersister.class
})
@Requires(classes = ReactiveServiceRegistryBuilder.class)
@Prototype
final class ReactiveRegistryCreator implements StandardServiceRegistryBuilderCreator {

    private final BootstrapServiceRegistry bootstrapServiceRegistry;

    public ReactiveRegistryCreator(@Primary @Nullable Integrator integrator, ApplicationContext applicationContext) {
        BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
        bootstrapServiceRegistryBuilder.applyClassLoader(applicationContext.getClassLoader());
        if (integrator != null) {
            bootstrapServiceRegistryBuilder.applyIntegrator(integrator);
        }
        this.bootstrapServiceRegistry = bootstrapServiceRegistryBuilder.build();
    }

    @Override
    public StandardServiceRegistryBuilder create(JpaConfiguration jpaConfiguration) {
        return jpaConfiguration.isReactive() ? new ReactiveServiceRegistryBuilder(bootstrapServiceRegistry) :
                new StandardServiceRegistryBuilder(bootstrapServiceRegistry);
    }

}
