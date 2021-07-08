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
package io.micronaut.configuration.hibernate.jpa

import io.micronaut.configuration.hibernate.jpa.other.Author
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.transaction.annotation.TransactionalAdvice
import io.micronaut.transaction.hibernate5.HibernateTransactionManager
import io.micronaut.transaction.jdbc.DelegatingDataSource
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.hibernate.Session
import org.hibernate.SessionFactory
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.sql.DataSource

/**
 * @author graemerocher
 * @since 1.0
 */
class MultipleDataSourceJpaSetupSpec extends Specification {

    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.other.url': 'jdbc:h2:mem:OTHERDB;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
            'jpa.other.packages-to-scan': 'io.micronaut.configuration.hibernate.jpa.other',
            'jpa.default.properties.hibernate.hbm2ddl.auto': 'create-drop'
    )

    void "test multiple data sources setup"() {
        given:
        SessionFactory defaultSessionFactory = applicationContext.getBean(SessionFactory)
        HibernateTransactionManager defaultTxManager = applicationContext.getBean(HibernateTransactionManager)

        SessionFactory otherSessionFactory = applicationContext.getBean(SessionFactory, Qualifiers.byName("other"))
        HibernateTransactionManager otherTxManager = applicationContext.getBean(HibernateTransactionManager, Qualifiers.byName("other"))

        expect:
        defaultSessionFactory != otherSessionFactory
        defaultSessionFactory.getMetamodel().entity(Book)
        otherSessionFactory.getMetamodel().entity(Author)
        defaultTxManager.dataSource == DelegatingDataSource.unwrapDataSource(applicationContext.getBean(DataSource))
        otherTxManager.dataSource == DelegatingDataSource.unwrapDataSource(applicationContext.getBean(DataSource, Qualifiers.byName('other')))
        defaultTxManager.sessionFactory == defaultSessionFactory
        otherTxManager.sessionFactory == otherSessionFactory
        defaultSessionFactory.jdbcServices.jdbcEnvironment.currentCatalog.toString() == "MYDB"
        otherSessionFactory.jdbcServices.jdbcEnvironment.currentCatalog.toString() == "OTHERDB"
    }

    void "test multiple data source transactional"() {
        given:
        MultipleDataSourceService service = applicationContext.getBean(MultipleDataSourceService)
        MutipleDataSourceJavaService javaService = applicationContext.getBean(MutipleDataSourceJavaService)

        expect: "Methods that retrieve the current session don't throw an exception"
        service.testOther()
        service.testCurrent()
        service.testContextOther()
        service.testViaSF()
        service.testEM()
        service.testContext()
        javaService.testCurrent()
        javaService.testCurrentFromField()
    }

    void "test that the connection pool is not exhaused and connections are closed"() {
        given:
        MultipleDataSourceService service = applicationContext.getBean(MultipleDataSourceService)
        expect:
        200.times { service.testOther() }
        200.times { service.testCurrent() }
    }

    @Singleton
    static class MultipleDataSourceService {
        @Inject
        Session session

        @Inject
        EntityManager em

        @Inject
        @Named("other")
        Session otherSession

        @Inject
        @Named("other")
        SessionFactory sessionFactory

        @Inject
        @PersistenceContext
        Session contextSession

        @Inject
        @Named("other")
        @PersistenceContext(name = "other")
        Session contextOther

        @TransactionalAdvice
        boolean testCurrent() {
            session.clear()
            return true
        }

        @TransactionalAdvice
        boolean testContext() {
            contextSession.clear()
            return true
        }

        @TransactionalAdvice("other")
        boolean testContextOther() {
            contextOther.clear()
            return true
        }

        @TransactionalAdvice
        boolean testEM() {
            em.clear()
            return true
        }

        @TransactionalAdvice("other")
        boolean testOther() {
            otherSession.clear()
            return true
        }

        @TransactionalAdvice("other")
        boolean testViaSF() {
            sessionFactory.currentSession.clear()
            return true
        }
    }
}
