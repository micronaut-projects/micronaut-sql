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
package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.inject.qualifiers.Qualifiers
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(DataSource)
        !applicationContext.containsBean(DatasourceConfiguration)

        cleanup:
        applicationContext.close()
    }

    void "test blank configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        DataSource dataSource = applicationContext.getBean(DataSource).targetDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.name == 'default'
        dataSource.driverClassName == 'org.h2.Driver'


        cleanup:
        applicationContext.close()
    }

    void "test properties are bindable"() {
        given:
        String context = UUID.randomUUID().toString()
        ApplicationContext applicationContext = new DefaultApplicationContext(context)
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                [
                        'datasources.default.initialPoolSize'          : 5,
                        'datasources.default.minPoolSize'              : 5,
                        'datasources.default.maxPoolSize'              : 20,
                        'datasources.default.timeoutCheckInterval'     : 5,
                        'datasources.default.inactiveConnectionTimeout': 10,
                        'datasources.default.connectionWaitTimeout'    : 10,
                        'datasources.default.loginTimeout'             : 20,
                ]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        DataSource dataSource = applicationContext.getBean(DataSource).targetDataSource

        then:
        dataSource.initialPoolSize == 5
        dataSource.minPoolSize == 5
        dataSource.maxPoolSize == 20
        dataSource.timeoutCheckInterval == 5
        dataSource.inactiveConnectionTimeout == 10
        dataSource.connectionWaitTimeout == 10
        dataSource.loginTimeout == 20

        cleanup:
        applicationContext.close()
    }

    void "test multiple data sources are configured"() {
        given:
        String context = UUID.randomUUID().toString()
        ApplicationContext applicationContext = new DefaultApplicationContext(context)
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                context,
                ['datasources.default': [:],
                 'datasources.foo'    : [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        DataSource dataSource = applicationContext.getBean(DataSource).targetDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.name == 'default'
        dataSource.driverClassName == 'org.h2.Driver'

        when:
        dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("foo")).targetDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.name == 'foo'
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }


    void "test multiple datasources are all wired"() {
        given:
        DataSource dataSource
        String context = UUID.randomUUID().toString()
        ApplicationContext applicationContext = new DefaultApplicationContext(context)
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                context,
                [
                        'datasources.default.initialPoolSize'          : 5,
                        'datasources.default.minPoolSize'              : 5,
                        'datasources.default.maxPoolSize'              : 20,
                        'datasources.default.timeoutCheckInterval'     : 5,
                        'datasources.default.inactiveConnectionTimeout': 10,
                        'datasources.default.connectionWaitTimeout'    : 10,
                        'datasources.default.loginTimeout'             : 20,

                        'datasources.person.initialPoolSize'           : 5,
                        'datasources.person.minPoolSize'               : 5,
                        'datasources.person.maxPoolSize'               : 20,
                        'datasources.person.timeoutCheckInterval'      : 5,
                        'datasources.person.inactiveConnectionTimeout' : 10,
                        'datasources.person.connectionWaitTimeout'     : 10,
                        'datasources.person.loginTimeout'              : 20,
                ]
        ))
        applicationContext.start()

        expect:
        applicationContext.getBeansOfType(DataSource).size() == 2
        applicationContext.getBeansOfType(DatasourceConfiguration).size() == 2

        when:
        dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("default")).targetDataSource

        then:
        dataSource.initialPoolSize == 5
        dataSource.minPoolSize == 5
        dataSource.maxPoolSize == 20
        dataSource.timeoutCheckInterval == 5
        dataSource.inactiveConnectionTimeout == 10
        dataSource.connectionWaitTimeout == 10
        dataSource.loginTimeout == 20

        when:
        dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("person")).targetDataSource

        then:
        dataSource.initialPoolSize == 5
        dataSource.minPoolSize == 5
        dataSource.maxPoolSize == 20
        dataSource.timeoutCheckInterval == 5
        dataSource.inactiveConnectionTimeout == 10
        dataSource.connectionWaitTimeout == 10
        dataSource.loginTimeout == 20

        cleanup:
        applicationContext.close()
    }

    void "test pool is created without dupe properties"() {
        given:
        String context = UUID.randomUUID().toString()
        ApplicationContext applicationContext = new DefaultApplicationContext(context)
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                context,
                [
                        'datasources.default.initialPoolSize'          : 5,
                        'datasources.default.minPoolSize'              : 5,
                        'datasources.default.maxPoolSize'              : 20,
                        'datasources.default.timeoutCheckInterval'     : 5,
                        'datasources.default.inactiveConnectionTimeout': 10,
                        'datasources.default.connectionWaitTimeout'    : 10,
                        'datasources.default.loginTimeout'             : 20,

                        'datasources.person.initialPoolSize'           : 5,
                        'datasources.person.minPoolSize'               : 5,
                        'datasources.person.maxPoolSize'               : 20,
                        'datasources.person.timeoutCheckInterval'      : 5,
                        'datasources.person.inactiveConnectionTimeout' : 10,
                        'datasources.person.connectionWaitTimeout'     : 10,
                        'datasources.person.loginTimeout'              : 20,
                ]
        ))
        applicationContext.start()

        when:
        PoolDataSource dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("person")).targetDataSource

        then:
        dataSource
    }

    void "test pool is created"() {
        given:
        String context = UUID.randomUUID().toString()
        ApplicationContext applicationContext = new DefaultApplicationContext(context)
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                context,
                [
                        'datasources.default.initialPoolSize'          : 5,
                        'datasources.default.minPoolSize'              : 5,
                        'datasources.default.maxPoolSize'              : 20,
                        'datasources.default.timeoutCheckInterval'     : 5,
                        'datasources.default.inactiveConnectionTimeout': 10,
                        'datasources.default.connectionWaitTimeout'    : 10,
                        'datasources.default.loginTimeout'             : 20,

                        'datasources.person.initialPoolSize'           : 5,
                        'datasources.person.minPoolSize'               : 5,
                        'datasources.person.maxPoolSize'               : 20,
                        'datasources.person.timeoutCheckInterval'      : 5,
                        'datasources.person.inactiveConnectionTimeout' : 10,
                        'datasources.person.connectionWaitTimeout'     : 10,
                        'datasources.person.loginTimeout'              : 20,
                ]
        ))
        applicationContext.start()

        when:
        PoolDataSource dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("person")).targetDataSource

        then:
        dataSource
    }
}
