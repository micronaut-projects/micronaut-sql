/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanLocator
import io.micronaut.context.annotation.Retain
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.context.event.BeanDestroyedEvent
import io.micronaut.core.value.PropertyResolver
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import oracle.ucp.admin.UniversalConnectionPoolManager
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceFactorySpec extends Specification {

    def "the factory and its pools receive nothing bound to the context, so that development mode can retain them"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'     : 'jdbc:h2:mem:retainable;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username': 'sa',
                'datasources.default.password': ''
        ])

        when:
        Collection<BeanDefinition<?>> pools = applicationContext.getBeanDefinitions(DataSource)
        Collection<BeanDefinition<?>> closure = [applicationContext.getBeanDefinition(DatasourceFactory)] + pools +
                applicationContext.getBeanDefinitions(DatasourceConfiguration)

        then:
        // the factory receives no connection pool manager, whose bean receives a configuration implemented by introduction advice
        !pools.isEmpty()
        pools.every { it.stringValues(Retain, "invalidatedBy") == ["datasources", "micronaut.application", "ucp-manager"] as String[] }
        closure.every { BeanDefinition<?> definition ->
            !definition.proxy && definition.requiredComponents.every { Class<?> type ->
                !BeanLocator.isAssignableFrom(type)
                        && !PropertyResolver.isAssignableFrom(type)
                        && !ApplicationEventPublisher.isAssignableFrom(type)
            }
        }

        cleanup:
        applicationContext.close()
    }

    void "destroys the pool of a data source once its bean is destroyed"() {
        given:
        ApplicationContext applicationContext = startContext('destroyedPool')
        DatasourceFactory factory = applicationContext.getBean(DatasourceFactory)
        UniversalConnectionPoolManager manager = applicationContext.getBean(UniversalConnectionPoolManager)
        String poolName = openPool(applicationContext)

        when: "a data source bean this factory did not produce is destroyed"
        factory.onDestroyed(new BeanDestroyedEvent(applicationContext, foreignDefinition(), Mock(DataSource)))

        then:
        manager.connectionPoolNames.contains(poolName)

        when: "the data source bean this factory produced is destroyed"
        applicationContext.destroyBean(applicationContext.getBeanRegistration(DataSource, Qualifiers.byName('default')))

        then: "its pool is destroyed while the context is still running"
        applicationContext.isRunning()
        !manager.connectionPoolNames.contains(poolName)

        cleanup:
        applicationContext.close()
    }

    void "close() destroys the pools that remain"() {
        given:
        ApplicationContext applicationContext = startContext('remainingPool')
        UniversalConnectionPoolManager manager = applicationContext.getBean(UniversalConnectionPoolManager)
        String poolName = openPool(applicationContext)

        when:
        applicationContext.getBean(DatasourceFactory).close()

        then:
        !manager.connectionPoolNames.contains(poolName)

        cleanup:
        applicationContext.close()
    }

    void "with the manager disabled, stopping the context destroys the pool that the first connection started"() {
        given:
        UniversalConnectionPoolManager manager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        ApplicationContext first = startContextWithoutManager('disabledManager')
        String poolName = openPool(first)

        expect: "UCP registers the pool with the manager of the JVM, whatever ucp-manager.enabled says"
        !first.containsBean(UniversalConnectionPoolManager)
        manager.connectionPoolNames.contains(poolName)
        sessions(first) == 1

        when:
        first.close()

        then:
        !manager.connectionPoolNames.contains(poolName)

        when: "another context in this JVM uses the name"
        ApplicationContext second = startContextWithoutManager('disabledManager')
        openPool(second)

        then:
        noExceptionThrown()
        sessions(second) == 1

        cleanup:
        first?.close()
        second?.close()
    }

    void "with the manager disabled, a data source that opened no connection leaves the pool of its name alone"() {
        given:
        UniversalConnectionPoolManager manager = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager()
        ApplicationContext running = startContextWithoutManager('unopenedSameName')
        String poolName = openPool(running)
        ApplicationContext unopened = startContextWithoutManager('unopenedSameName')

        when:
        unopened.close()

        then:
        manager.connectionPoolNames.contains(poolName)
        sessions(running) == 1

        cleanup:
        unopened?.close()
        running?.close()
    }

    private static ApplicationContext startContext(String database) {
        return ApplicationContext.run([
                "datasources.default.url"     : "jdbc:h2:mem:${database};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE".toString(),
                "datasources.default.username": "sa",
                "datasources.default.password": ""
        ], "test")
    }

    private static ApplicationContext startContextWithoutManager(String database) {
        return ApplicationContext.run([
                "ucp-manager.enabled"         : false,
                "datasources.default.url"     : "jdbc:h2:mem:${database};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE".toString(),
                "datasources.default.username": "sa",
                "datasources.default.password": ""
        ], "test")
    }

    private static String openPool(ApplicationContext applicationContext) {
        DataSourceResolver resolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)
        PoolDataSource pool = (PoolDataSource) resolver.resolve(applicationContext.getBean(DataSource))
        pool.getConnection().close()
        return pool.connectionPoolName
    }

    private static int sessions(ApplicationContext applicationContext) {
        applicationContext.getBean(DataSource).connection.withCloseable { connection ->
            connection.createStatement().withCloseable { statement ->
                statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS").withCloseable { rows ->
                    rows.next()
                    return rows.getInt(1)
                }
            }
        }
    }

    private BeanDefinition<DataSource> foreignDefinition() {
        BeanDefinition<DataSource> definition = Mock()
        definition.getDeclaringType() >> Optional.of(String)
        definition.getDeclaredQualifier() >> Qualifiers.byName('default')
        return definition
    }
}
