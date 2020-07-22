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
package io.micronaut.configuration.hibernate.jpa;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micronaut.configuration.hibernate.jpa.condition.RequiresHibernateEntities;
import io.micronaut.context.BeanLocator;
import io.micronaut.context.annotation.*;
import io.micronaut.context.env.Environment;
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.transaction.hibernate5.MicronautSessionContext;
import org.hibernate.Interceptor;
import org.hibernate.MappingException;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.SessionFactoryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.resource.beans.container.spi.BeanContainer;
import org.hibernate.resource.beans.container.spi.ContainedBean;
import org.hibernate.resource.beans.spi.BeanInstanceProducer;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.validation.ValidatorFactory;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A factory bean for constructing the {@link javax.persistence.EntityManagerFactory}.
 *
 * @author graemerocher
 * @since 1.0
 */
@Factory
public class EntityManagerFactoryBean {

    private static final Logger LOG = LoggerFactory.getLogger(EntityManagerFactoryBean.class);

    private final JpaConfiguration jpaConfiguration;
    private final Environment environment;
    private final BeanLocator beanLocator;
    private Interceptor hibernateInterceptor;

    /**
     * @param jpaConfiguration The JPA configuration
     * @param environment      The environment
     * @param beanLocator      The bean locator
     */
    public EntityManagerFactoryBean(
            JpaConfiguration jpaConfiguration,
            Environment environment,
            BeanLocator beanLocator) {

        this.jpaConfiguration = jpaConfiguration;
        this.environment = environment;
        this.beanLocator = beanLocator;
    }

    /**
     * Sets the {@link Interceptor} to use.
     *
     * @param hibernateInterceptor The hibernate interceptor
     */
    @Inject
    public void setHibernateInterceptor(@Nullable Interceptor hibernateInterceptor) {
        this.hibernateInterceptor = hibernateInterceptor;
    }

    /**
     * Builds the {@link StandardServiceRegistry} bean for the given {@link DataSource}.
     *
     * @param dataSourceName The data source name
     * @param dataSource     The data source
     * @return The {@link StandardServiceRegistry}
     */
    @EachBean(DataSource.class)
    protected StandardServiceRegistry hibernateStandardServiceRegistry(
        @Parameter String dataSourceName,
        DataSource dataSource) {

        final DataSourceResolver dataSourceResolver = beanLocator.findBean(DataSourceResolver.class).orElse(null);
        if (dataSourceResolver != null) {
            dataSource = dataSourceResolver.resolve(dataSource);
        }

        Map<String, Object> additionalSettings = new LinkedHashMap<>();
        additionalSettings.put(AvailableSettings.DATASOURCE, dataSource);
        Class<?> sessionContextClass = ClassUtils.forName("org.springframework.orm.hibernate5"
                                                        + ".SpringSessionContext", EntityManagerFactoryBean.class.getClassLoader())
                .orElse(MicronautSessionContext.class);
        additionalSettings.put(
                AvailableSettings.CURRENT_SESSION_CONTEXT_CLASS, sessionContextClass.getName());
        additionalSettings.put(AvailableSettings.SESSION_FACTORY_NAME, dataSourceName);
        additionalSettings.put(AvailableSettings.SESSION_FACTORY_NAME_IS_JNDI, false);
        additionalSettings.put(AvailableSettings.BEAN_CONTAINER, new BeanContainer() {
            @Override
            public <B> ContainedBean<B> getBean(Class<B> beanType, LifecycleOptions lifecycleOptions, BeanInstanceProducer fallbackProducer) {
                B bean = beanLocator.findBean(beanType)
                        .orElseGet(() -> fallbackProducer.produceBeanInstance(beanType));
                return () -> bean;
            }

            @Override
            public <B> ContainedBean<B> getBean(
                    String name,
                    Class<B> beanType,
                    LifecycleOptions lifecycleOptions,
                    BeanInstanceProducer fallbackProducer) {
                B bean = beanLocator.findBean(beanType, Qualifiers.byName(name))
                        .orElseGet(() -> fallbackProducer.produceBeanInstance(name, beanType));
                return () -> bean;
            }

            @Override
            public void stop() {
                // no-op, managed externally
            }
        });
        JpaConfiguration jpaConfiguration = beanLocator.findBean(JpaConfiguration.class, Qualifiers.byName(dataSourceName))
            .orElse(this.jpaConfiguration);
        return jpaConfiguration.buildStandardServiceRegistry(
            additionalSettings
        );
    }

    /**
     * Builds the {@link MetadataSources} for the given {@link StandardServiceRegistry}.
     *
     * @param jpaConfiguration        The JPA configuration
     * @param standardServiceRegistry The standard service registry
     * @return The {@link MetadataSources}
     */
    @EachBean(StandardServiceRegistry.class)
    @RequiresHibernateEntities
    protected MetadataSources hibernateMetadataSources(
        @Parameter JpaConfiguration jpaConfiguration,
        StandardServiceRegistry standardServiceRegistry) {

        MetadataSources metadataSources = createMetadataSources(standardServiceRegistry);
        JpaConfiguration.EntityScanConfiguration entityScanConfiguration = jpaConfiguration.getEntityScanConfiguration();
        entityScanConfiguration.findEntities().forEach(metadataSources::addAnnotatedClass);
        return metadataSources;
    }

    /**
     * Builds the {@link SessionFactoryBuilder} to use.
     *
     * @param metadataSources  The {@link MetadataSources}
     * @param validatorFactory The {@link ValidatorFactory}
     * @return The {@link SessionFactoryBuilder}
     */
    @EachBean(MetadataSources.class)
    @Requires(beans = MetadataSources.class)
    protected SessionFactoryBuilder hibernateSessionFactoryBuilder(
        MetadataSources metadataSources,
        @Nullable ValidatorFactory validatorFactory) {

        try {
            Metadata metadata = metadataSources.buildMetadata();
            SessionFactoryBuilder sessionFactoryBuilder = metadata.getSessionFactoryBuilder();
            if (validatorFactory != null) {
                sessionFactoryBuilder.applyValidatorFactory(validatorFactory);
            }

            if (hibernateInterceptor != null) {
                sessionFactoryBuilder.applyInterceptor(hibernateInterceptor);
            }
            return sessionFactoryBuilder;
        } catch (MappingException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Hibernate mapping error", e);
            }
            throw e;
        } catch (Exception e) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Error creating SessionFactoryBuilder", e);
            }
            throw e;
        }
    }

    /**
     * Builds the actual {@link SessionFactory} from the builder.
     *
     * @param sessionFactoryBuilder The {@link SessionFactoryBuilder}
     * @return The {@link SessionFactory}
     */
    @Context
    @Requires(beans = SessionFactoryBuilder.class)
    @Bean(preDestroy = "close")
    @EachBean(SessionFactoryBuilder.class)
    protected SessionFactory hibernateSessionFactory(SessionFactoryBuilder sessionFactoryBuilder) {
        return sessionFactoryBuilder.build();
    }

    /**
     * Creates the {@link MetadataSources} for the given registry.
     *
     * @param serviceRegistry The registry
     * @return The sources
     */
    @SuppressWarnings("WeakerAccess")
    protected MetadataSources createMetadataSources(@Nonnull StandardServiceRegistry serviceRegistry) {
        return new MetadataSources(serviceRegistry);
    }
}
