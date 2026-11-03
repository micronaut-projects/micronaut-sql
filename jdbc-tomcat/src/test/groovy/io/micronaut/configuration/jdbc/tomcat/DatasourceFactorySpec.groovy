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
package io.micronaut.configuration.jdbc.tomcat

import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.BeanDestroyedEvent
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import org.apache.tomcat.jdbc.pool.ConnectionPool
import org.apache.tomcat.jdbc.pool.DataSource
import spock.lang.Specification

class DatasourceFactorySpec extends Specification {

    DatasourceFactory datasourceFactory

    def "create basic datasource"() {
        given:
        def dataSource = new DataSource(validationQuery: "SELECT 1")
        DatasourceFactory datasourceFactory = new DatasourceFactory(new DataSourceResolver() {
            @Override
            javax.sql.DataSource resolve(javax.sql.DataSource ds) {
                return ds
            }
        }, ApplicationContext.run())

        when:
        def metadata = datasourceFactory.tomcatPoolDataSourceMetadataProvider(dataSource)

        then:
        metadata
        metadata.idle >= 0
        metadata.max >= 0
        metadata.active >= 0
        metadata.getValidationQuery()
        metadata.usage >= 0
    }

    def "create transactional datasource"() {
        given:
        def dataSource = new DataSource(validationQuery: "SELECT 1")
        def proxyDataSource = Spy(dataSource)
        def dataSourceResolver = new DataSourceResolver() {
            @Override
            javax.sql.DataSource resolve(javax.sql.DataSource ds) {
                if (ds.is(proxyDataSource)) {
                    return dataSource
                }
                return ds
            }
        }

        when:
        def metadata = new DatasourceFactory(dataSourceResolver, ApplicationContext.run()).tomcatPoolDataSourceMetadataProvider(proxyDataSource)

        then:
        metadata
        metadata.idle >= 0
        metadata.max >= 0
        metadata.active >= 0
        metadata.validationQuery == "SELECT 1"
        metadata.usage >= 0
    }

    def "closes the pool of a data source once its bean is destroyed"() {
        given:
        ApplicationContext applicationContext = startContext('destroyedPool')
        DataSource dataSource = openPool(applicationContext)
        ConnectionPool pool = dataSource.pool

        when: "a data source bean this factory did not produce is destroyed"
        applicationContext.getBean(DatasourceFactory).onDestroyed(
                new BeanDestroyedEvent(applicationContext, foreignDefinition(), dataSource))

        then:
        !pool.isClosed()

        when: "the data source bean this factory produced is destroyed"
        applicationContext.destroyBean(applicationContext.getBeanRegistration(javax.sql.DataSource, Qualifiers.byName('default')))

        then: "its pool is closed while the context is still running"
        applicationContext.isRunning()
        pool.isClosed()

        cleanup:
        applicationContext.close()
    }

    def "close() closes the pools that remain open"() {
        given:
        ApplicationContext applicationContext = startContext('remainingPool')
        ConnectionPool pool = openPool(applicationContext).pool

        when:
        applicationContext.getBean(DatasourceFactory).close()

        then:
        pool.isClosed()

        cleanup:
        applicationContext.close()
    }

    private static ApplicationContext startContext(String database) {
        return ApplicationContext.run('datasources.default.url': "jdbc:h2:mem:${database};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE".toString())
    }

    private static DataSource openPool(ApplicationContext applicationContext) {
        DataSourceResolver resolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)
        DataSource pool = (DataSource) resolver.resolve(applicationContext.getBean(javax.sql.DataSource))
        pool.getConnection().close()
        return pool
    }

    private BeanDefinition<javax.sql.DataSource> foreignDefinition() {
        BeanDefinition<javax.sql.DataSource> definition = Mock()
        definition.getDeclaringType() >> Optional.of(String)
        definition.getDeclaredQualifier() >> Qualifiers.byName('default')
        return definition
    }
}
