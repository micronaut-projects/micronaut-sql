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
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(PoolDataSource)
        !applicationContext.containsBean(DatasourceConfiguration)

        cleanup:
        applicationContext.close()
    }

    void 'test set datasource properties'() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": "",
                        "datasources.default.data-source-properties": [
                                "oracle.fan.enabled": true
                        ]
                ]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(PoolDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.getURL() == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == 'sa'

        cleanup:
        applicationContext.close()
    }

    void "test blank configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": "",
                ]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(PoolDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.getURL() == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == 'sa'

        cleanup:
        applicationContext.close()
    }

    void "test datasource can be disabled"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": "",
                        "datasources.default.enabled": false
                ]
        ))
        applicationContext.start()

        when:
        def datasourceConfiguration = applicationContext.getBean(DatasourceConfiguration)
        then:
        datasourceConfiguration
        !datasourceConfiguration.enabled

        when:
        applicationContext.getBean(PoolDataSource)
        then:
        def exception = thrown(NoSuchBeanException)
        exception.message.contains('disabled since bean property [enabled] value is not equal to [true]')

        when:
        applicationContext.getBean(DataSource)

        then:
        exception = thrown(NoSuchBeanException)
        exception.message.startsWith("No bean of type [javax.sql.DataSource] exists.")
        exception.message.contains('disabled since bean property [enabled] value is not equal to [true]')

        cleanup:
        applicationContext.close()
    }

    void "test datasource can be disabled and enabled"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                [
                        'datasources.default': [:],
                        'datasources.default.enabled' : false,
                        'datasources.custom': [:],
                ]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DatasourceConfiguration)
        def defaultDatasourceConfiguration = applicationContext.getBean(DatasourceConfiguration)
        !defaultDatasourceConfiguration.enabled

        applicationContext.containsBean(DatasourceConfiguration, Qualifiers.byName('custom'))
        def customDatasourceConfiguration = applicationContext.getBean(DatasourceConfiguration, Qualifiers.byName('custom'))
        customDatasourceConfiguration.enabled

        when:
        applicationContext.getBean(DataSource)
        then:
        def exception = thrown(NoSuchBeanException)
        exception.message.contains('disabled since bean property [enabled] value is not equal to [true]')
        when:
        applicationContext.getBean(PoolDataSource)
        then:
        thrown(NoSuchBeanException)

        when:
        DataSource customDataSource = applicationContext.getBean(DataSource, Qualifiers.byName('custom'))
        then:
        noExceptionThrown()
        customDataSource

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(customDataSource)

        then: // The configuration is supplied because H2 is on the classpath
        dataSource.getURL() == 'jdbc:h2:mem:custom;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == 'sa'

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
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(PoolDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then:
        dataSource.getInitialPoolSize() == 5
        dataSource.getMinPoolSize() == 5
        dataSource.getMaxPoolSize() == 20
        dataSource.getTimeoutCheckInterval() == 5
        dataSource.getInactiveConnectionTimeout() == 10
        dataSource.getConnectionWaitTimeout() == 10
        dataSource.getLoginTimeout() == 20

        cleanup:
        applicationContext.close()
    }

    void "test multiple data sources are configured"() {
        given:
        String context = UUID.randomUUID().toString()
        ApplicationContext applicationContext = new DefaultApplicationContext(context)
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                context,
                [
                    "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "datasources.default.username": "sa",
                    "datasources.default.password": "",
                    "datasources.foo.url": "jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                    "datasources.foo.username": "sa",
                    "datasources.foo.password": "",
                ]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.getURL() == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == 'sa'

        when:
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("foo")))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.getURL() == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == 'sa'

        cleanup:
        applicationContext.close()
    }


    void "test multiple datasources are all wired"() {
        given:
        PoolDataSource dataSource
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
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.getBeansOfType(DataSource).size() == 2
        applicationContext.getBeansOfType(DatasourceConfiguration).size() == 2

        when:
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("default")))

        then:
        dataSource.getInitialPoolSize() == 5
        dataSource.getMinPoolSize() == 5
        dataSource.getMaxPoolSize() == 20
        dataSource.getTimeoutCheckInterval() == 5
        dataSource.getInactiveConnectionTimeout() == 10
        dataSource.getConnectionWaitTimeout() == 10
        dataSource.getLoginTimeout() == 20

        when:
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("person")))

        then:
        dataSource.getInitialPoolSize() == 5
        dataSource.getMinPoolSize() == 5
        dataSource.getMaxPoolSize() == 20
        dataSource.getTimeoutCheckInterval() == 5
        dataSource.getInactiveConnectionTimeout() == 10
        dataSource.getConnectionWaitTimeout() == 10
        dataSource.getLoginTimeout() == 20

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
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("person")))

        then:
        dataSource

        cleanup:
        applicationContext.close()
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
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("person")))

        then:
        dataSource

        cleanup:
        applicationContext.close()
    }

    void "test pool is created with calculated settings"() {

        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ["datasources.default.data-source-properties": ["oracle.fan.enabled": true]]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(PoolDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.getSQLForValidateConnection()== 'SELECT 1'
        dataSource.getConnectionFactoryClassName() == 'org.h2.Driver'
        dataSource.getURL() == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == "sa"

        cleanup:
        applicationContext.close()
    }
}
