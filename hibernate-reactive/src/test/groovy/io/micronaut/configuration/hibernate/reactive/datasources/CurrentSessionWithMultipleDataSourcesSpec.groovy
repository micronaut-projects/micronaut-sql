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
package io.micronaut.configuration.hibernate.reactive.datasources


import io.micronaut.configuration.hibernate.reactive.datasources.db1.ProductRepository
import io.micronaut.configuration.hibernate.reactive.datasources.db2.BookstoreMethodLevelTransaction
import io.micronaut.configuration.hibernate.reactive.datasources.db2.BookstoreRepository
import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.BeanInstantiationException
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import org.hibernate.SessionFactory
import spock.lang.Specification

class CurrentSessionWithMultipleDataSourcesSpec extends Specification {

    void "test an application that defines 2 datasources uses correct transaction management"() {
        given:
            def context = ApplicationContext.run(
                    'datasources.default.name': 'db1',
                    'datasources.db2.url': 'jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                    'jpa.default.entity-scan.packages': ['io.micronaut.configuration.hibernate.reactive.datasources.db1'],
                    'jpa.default.properties.hibernate.hbm2ddl.auto':'create-drop',
                    'jpa.db2.properties.hibernate.hbm2ddl.auto':'create-drop',
                    'jpa.db2.entity-scan.packages': ['io.micronaut.configuration.hibernate.reactive.datasources.db2'],
            )

            ProductRepository productRepository = context.getBean(ProductRepository)
            BookstoreRepository bookstoreRepository = context.getBean(BookstoreRepository)
            BookstoreMethodLevelTransaction methodLevelTransaction = context.getBean(BookstoreMethodLevelTransaction)

        when:"Some data is saved in each database"
            def store1 = bookstoreRepository.save("Waterstones")
            def store2 = bookstoreRepository.save("Amazon")
            def product1 = productRepository.save("1234", "The Stand")
            def product2 = productRepository.save("1234", "The Stand")

        then:"The data is available"
            bookstoreRepository.findById(store1.id).isPresent()
            bookstoreRepository.findById(store2.id).isPresent()
            productRepository.findById(product1.id).isPresent()
            productRepository.findById(product2.id).isPresent()
            methodLevelTransaction.findById(store1.id).isPresent()
            methodLevelTransaction.findById(store2.id).isPresent()

        cleanup:
            context.close()
    }

    void "test an application that defines multiple data sources"() {
        when:
            def context = ApplicationContext.run(
                    'datasources.default.name': 'db1',
                    'datasources.db2.url': 'jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                    'datasources.abc.url': 'jdbc:h2:mem:db2;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                    'jpa.db2.entity-scan.packages': ['io.micronaut.configuration.hibernate.reactive.datasources.db2'],
            )
        then:
            !context.findBean(SessionFactory, Qualifiers.byName("default")).isPresent()
            context.findBean(SessionFactory, Qualifiers.byName("db2")).isPresent()
            !context.findBean(SessionFactory, Qualifiers.byName("abc")).isPresent()
            !context.findBean(SessionFactory, Qualifiers.byName("unknown")).isPresent()
        cleanup:
            context.close()
    }

    void "test parallel init of non-default SessionFactory with missing entities"() {
        when:
            def context = ApplicationContext.run(
                    'datasources.xyz.name': 'db1',
                    'jpa.xyz.entity-scan.packages': ['doesntexist']
            )
        then:
            def e = thrown(BeanInstantiationException)
            e.message.contains "Entities not found for JPA configuration: 'xyz'"

    }

    void "test eager get of non-default SessionFactory with missing entities"() {
        when:
            ApplicationContext.run(
                    'datasources.xyz.name': 'db1',
                    'jpa.xyz.entity-scan.packages': ['doesntexist']
            ).getBean(SessionFactory, Qualifiers.byName("xyz"))
        then:
            def e = thrown(BeanInstantiationException)
            e.message.contains "Entities not found for JPA configuration: 'xyz'"
    }

    void "test parallel init of SessionFactory with missing entities2"() {
        when:
            ApplicationContext.run(
                    'datasources.default.name': 'db1',
                    'jpa.default.entity-scan.packages': ['doesntexist']
            )
        then:
            def e = thrown(BeanInstantiationException)
            e.message.contains "Entities not found for JPA configuration: 'default'"

    }

    void "test eager get SessionFactory with empty entity-scan"() {
        when:
            ApplicationContext.run(
                    'datasources.default.name': 'db1',
                    'jpa.default.entity-scan.packages': ['doesntexist']
            ).getBean(SessionFactory, Qualifiers.byName("default"))
        then:
            def e = thrown(BeanInstantiationException)
            e.message.contains "Entities not found for JPA configuration: 'default'"
    }

    void "test get SessionFactory without configured default"() {
        when:
            ApplicationContext.run().getBean(SessionFactory, Qualifiers.byName("default"))
        then:
            def e = thrown(NoSuchBeanException)
            e.message.contains "No bean of type [org.hibernate.SessionFactory] exists for the given qualifier: @Named('default')"
    }

    void "test get SessionFactory without any configuration"() {
        when:
            ApplicationContext.run().getBean(SessionFactory)
        then:
            def e = thrown(NoSuchBeanException)
            e.message.contains "No bean of type [org.hibernate.SessionFactory] exists."
    }

    void "test get SessionFactory with only configured default data source name"() {
        when:
            ApplicationContext.run(
                    'datasources.default.name': 'db1',
            ).getBean(SessionFactory, Qualifiers.byName("default"))
        then:
            thrown(NoSuchBeanException)
    }

    void "test get SessionFactory with only configured default data source name when jpa is also configured"() {
        when:
            ApplicationContext.run(
                    'datasources.default.name': 'db1',
                    'jpa.default': [],
            ).getBean(SessionFactory, Qualifiers.byName("default"))
        then:
            noExceptionThrown()
    }

}
