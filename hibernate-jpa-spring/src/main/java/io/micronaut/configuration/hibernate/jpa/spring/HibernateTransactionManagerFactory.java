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
package io.micronaut.configuration.hibernate.jpa.spring;

import io.micronaut.context.annotation.*;
import io.micronaut.core.annotation.TypeHint;
import io.micronaut.jdbc.spring.DataSourceTransactionManagerFactory;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate5.HibernateTransactionManager;
import org.springframework.orm.hibernate5.SpringSessionContext;

import javax.sql.DataSource;

/**
 * Sets up the default hibernate transaction manager.
 *
 * @author graemerocher
 * @since 1.0
 */
@Factory
@Requires(classes = HibernateTransactionManager.class)
@Replaces(factory = DataSourceTransactionManagerFactory.class)
@TypeHint(SpringSessionContext.class)
public class HibernateTransactionManagerFactory {

    /**
     * @param sessionFactory The {@link SessionFactory}
     * @param dataSource     The {@link DataSource}
     * @return The {@link HibernateTransactionManager}
     */
    @Bean
    @Requires(classes = HibernateTransactionManager.class)
    @EachBean(SessionFactory.class)
    HibernateTransactionManager hibernateTransactionManager(SessionFactory sessionFactory, @Parameter DataSource dataSource) {
        HibernateTransactionManager hibernateTransactionManager = new HibernateTransactionManager(sessionFactory);
        hibernateTransactionManager.setDataSource(dataSource);
        return hibernateTransactionManager;
    }
}
