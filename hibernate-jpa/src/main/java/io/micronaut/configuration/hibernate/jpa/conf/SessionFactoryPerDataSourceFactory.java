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
package io.micronaut.configuration.hibernate.jpa.conf;

import io.micronaut.configuration.hibernate.jpa.JpaConfiguration;
import io.micronaut.configuration.hibernate.jpa.conf.serviceregistry.builder.configures.StandardServiceRegistryBuilderConfigurer;
import io.micronaut.configuration.hibernate.jpa.conf.serviceregistry.builder.supplier.StandardServiceRegistryBuilderCreator;
import io.micronaut.configuration.hibernate.jpa.conf.sessionfactory.configure.SessionFactoryBuilderConfigurer;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.core.annotation.TypeHint;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.ServiceRegistry;

import javax.sql.DataSource;
import java.util.List;

/**
 * Factory that builds {@link SessionFactory} per {@link DataSource}.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Internal
@Requires(missingClasses = "org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder")
@TypeHint(typeNames = "org.hibernate.reactive.provider.ReactiveServiceRegistryBuilder")
@Factory
final class SessionFactoryPerDataSourceFactory extends AbstractHibernateFactory {

    private final JpaConfiguration defaultJpaConfiguration;

    SessionFactoryPerDataSourceFactory(Environment environment,
                                       List<SessionFactoryBuilderConfigurer> configures,
                                       StandardServiceRegistryBuilderCreator serviceRegistryBuilderSupplier,
                                       List<StandardServiceRegistryBuilderConfigurer> standardServiceRegistryBuilderConfigurers,
                                       @Primary @Nullable JpaConfiguration jpaConfiguration,
                                       ApplicationContext applicationContext,
                                       @Primary @Nullable Integrator integrator) {
        super(environment, configures, serviceRegistryBuilderSupplier, standardServiceRegistryBuilderConfigurers);
        this.defaultJpaConfiguration = jpaConfiguration != null ? jpaConfiguration : new JpaConfiguration(applicationContext, integrator);
    }

    @EachBean(DataSource.class)
    ServiceRegistry buildHibernateStandardServiceRegistry(@Parameter @Nullable JpaConfiguration jpaConfiguration,
                                                          @Parameter String name) {
        if (jpaConfiguration == null) {
            jpaConfiguration = defaultJpaConfiguration.copy(name);
        }
        return super.buildHibernateStandardServiceRegistry(jpaConfiguration);
    }

    @EachBean(ServiceRegistry.class)
    MetadataSources buildMetadataSources(ServiceRegistry serviceRegistry) {
        return super.buildMetadataSources(serviceRegistry);
    }

    @EachBean(MetadataSources.class)
    Metadata buildMetadata(MetadataSources metadataSources,
                           @Parameter @Nullable JpaConfiguration jpaConfiguration,
                           @Parameter String name) {
        if (jpaConfiguration == null) {
            jpaConfiguration = defaultJpaConfiguration.copy(name);
        }
        return super.buildMetadata(metadataSources, jpaConfiguration);
    }

    @EachBean(Metadata.class)
    SessionFactoryBuilder buildHibernateSessionFactoryBuilder(Metadata metadata,
                                                              @Parameter @Nullable JpaConfiguration jpaConfiguration,
                                                              @Parameter String name) {
        if (jpaConfiguration == null) {
            jpaConfiguration = defaultJpaConfiguration.copy(name);
        }
        return super.buildHibernateSessionFactoryBuilder(metadata, jpaConfiguration);
    }

    @Bean(preDestroy = "close")
    @Context
    @EachBean(SessionFactoryBuilder.class)
    SessionFactory buildHibernateSessionFactoryBuilder(SessionFactoryBuilder sessionFactoryBuilder) {
        return super.buildHibernateSessionFactory(sessionFactoryBuilder);
    }

}
