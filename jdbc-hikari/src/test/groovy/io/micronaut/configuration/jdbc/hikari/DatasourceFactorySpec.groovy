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
package io.micronaut.configuration.jdbc.hikari

import com.zaxxer.hikari.HikariDataSource
import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanLocator
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.context.event.BeanDestroyedEvent
import io.micronaut.core.value.PropertyResolver
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceFactorySpec extends Specification {

    def "wire class with constructor"() {
        expect:
        new DatasourceFactory()
    }

    def "the factory and its pools receive nothing bound to the context, so that development mode can retain them"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(['datasources.default': [:]])

        when:
        BeanDefinition<DatasourceFactory> factory = applicationContext.getBeanDefinition(DatasourceFactory)
        Collection<BeanDefinition<?>> pools = applicationContext.getBeanDefinitions(javax.sql.DataSource)

        then:
        factory.constructor.arguments.length == 0
        !pools.isEmpty()
        pools.every { it.stringValues(io.micronaut.context.annotation.Retain, "invalidatedBy") == ["datasources", "micronaut.application"] as String[] }
        ([factory] + pools + applicationContext.getBeanDefinitions(DatasourceConfiguration)).every { BeanDefinition<?> definition ->
            definition.requiredComponents.every { Class<?> type ->
                !BeanLocator.isAssignableFrom(type)
                        && !PropertyResolver.isAssignableFrom(type)
                        && !ApplicationEventPublisher.isAssignableFrom(type)
            }
        }

        cleanup:
        applicationContext.close()
    }

    def "closes the pool of a data source once its bean is destroyed"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'datasources.default.url': 'jdbc:h2:mem:destroyedPool;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE')
        DatasourceFactory factory = applicationContext.getBean(DatasourceFactory)
        HikariDataSource pool = resolvePool(applicationContext)

        when: "a data source bean this factory did not produce is destroyed"
        factory.onDestroyed(new BeanDestroyedEvent(applicationContext, foreignDefinition(), pool))

        then:
        !pool.isClosed()

        when: "a bean this factory produced is destroyed without a name"
        factory.onDestroyed(new BeanDestroyedEvent(applicationContext, factoryDefinition(null), pool))

        then:
        !pool.isClosed()

        when: "the data source bean this factory produced is destroyed"
        applicationContext.destroyBean(applicationContext.getBeanRegistration(DataSource, Qualifiers.byName('default')))

        then: "its pool is closed while the context is still running"
        applicationContext.isRunning()
        pool.isClosed()

        cleanup:
        applicationContext.close()
    }

    def "close() closes the pools that remain open"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'datasources.default.url': 'jdbc:h2:mem:remainingPool;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE')
        HikariDataSource pool = resolvePool(applicationContext)

        when:
        applicationContext.getBean(DatasourceFactory).close()

        then:
        pool.isClosed()

        cleanup:
        applicationContext.close()
    }

    private static HikariDataSource resolvePool(ApplicationContext applicationContext) {
        DataSourceResolver resolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)
        return (HikariDataSource) resolver.resolve(applicationContext.getBean(DataSource))
    }

    private BeanDefinition<DataSource> foreignDefinition() {
        BeanDefinition<DataSource> definition = Mock()
        definition.getDeclaringType() >> Optional.of(String)
        definition.getDeclaredQualifier() >> Qualifiers.byName('default')
        return definition
    }

    private BeanDefinition<DataSource> factoryDefinition(String name) {
        BeanDefinition<DataSource> definition = Mock()
        definition.getDeclaringType() >> Optional.of(DatasourceFactory)
        definition.getDeclaredQualifier() >> (name == null ? null : Qualifiers.byName(name))
        return definition
    }
}
