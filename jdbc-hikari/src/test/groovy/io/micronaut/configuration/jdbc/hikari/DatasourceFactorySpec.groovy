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
import io.micronaut.context.event.BeanDestroyedEvent
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceFactorySpec extends Specification {

    def "wire class with constructor"() {
        expect:
        new DatasourceFactory(Mock(ApplicationContext))
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

        when:
        applicationContext.stop()

        then:
        pool.isClosed()
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
