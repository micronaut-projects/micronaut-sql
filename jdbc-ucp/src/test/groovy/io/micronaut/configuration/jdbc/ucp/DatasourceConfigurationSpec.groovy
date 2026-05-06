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

import io.micronaut.configuration.jdbc.ucp.metadata.OracleUcpDataSourcePoolMetadata
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.util.Util
import spock.lang.Ignore
import spock.lang.Specification

import javax.sql.DataSource
import java.time.Duration

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(PoolDataSource)
        !applicationContext.containsBean(DatasourceConfiguration)
        !applicationContext.containsBean(OracleUcpDataSourcePoolMetadata)

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
                        "datasources.default.data-source-properties.oracle.fan.enabled": true
                ]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(PoolDataSource)
        applicationContext.containsBean(DatasourceConfiguration)
        applicationContext.containsBean(OracleUcpDataSourcePoolMetadata)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.getURL() == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == 'sa'
        dataSource.getConnectionProperties().getProperty('oracle.fan.enabled') == 'true'

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
        applicationContext.containsBean(OracleUcpDataSourcePoolMetadata)

        when:
        PoolDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.getURL() == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.getUser() == 'sa'

        cleanup:
        applicationContext.close()
    }

    @Ignore("https://jira.oraclecorp.com/jira/browse/JDBC-4314")
    void "test default configuration and password change"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        System.setProperty("ds-default-password", "")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                [
                        'datasources.default.url': 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                        'datasources.default.username': 'sa',
                        'datasources.default.password': '${ds-default-password}',
                        'datasources.default.driver-class-name': 'org.h2.Driver',
                        'datasources.default.dialect': 'H2'
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

        when:"Fire datasource password change event"
        def newPassword = 'updated_pwd'
        System.setProperty("ds-default-password", newPassword)
        def changes = applicationContext.environment.refreshAndDiff()
        dataSource.connection.prepareStatement("ALTER USER sa SET PASSWORD '" + newPassword + "'").executeUpdate()
        applicationContext.publishEvent(new RefreshEvent(changes))
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then:"Password is updated and query can be executed"
        def newRs = dataSource.connection.prepareStatement('SELECT 1').executeQuery()
        newRs.next()
        newRs.getInt(1) == 1

        cleanup:
        // Change back to default password
        dataSource.connection.prepareStatement("ALTER USER sa SET PASSWORD ''").executeUpdate()
        System.setProperty("ds-default-password", "")
        changes = applicationContext.environment.refreshAndDiff()
        applicationContext.publishEvent(new RefreshEvent(changes))
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
        applicationContext.getBean(PoolDataSource, Qualifiers.byName('default'))
        then:
        thrown(NoSuchBeanException)

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('default'))

        then:
        thrown(NoSuchBeanException)

        when:
        applicationContext.getBean(OracleUcpDataSourcePoolMetadata, Qualifiers.byName('default'))
        then:
        thrown(NoSuchBeanException)

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

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('default'))
        then:
        thrown(NoSuchBeanException)
        when:
        applicationContext.getBean(PoolDataSource, Qualifiers.byName('default'))
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
                        'datasources.default.connectionWaitDuration'   : "10s",
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
        dataSource.getConnectionWaitDuration() == Duration.ofSeconds(10)
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
                        'datasources.default.connectionWaitDuration'   : "10s",
                        'datasources.default.loginTimeout'             : 20,

                        'datasources.person.initialPoolSize'           : 5,
                        'datasources.person.minPoolSize'               : 5,
                        'datasources.person.maxPoolSize'               : 20,
                        'datasources.person.timeoutCheckInterval'      : 5,
                        'datasources.person.inactiveConnectionTimeout' : 10,
                        'datasources.person.connectionWaitDuration'    : "10s",
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
        dataSource.getConnectionWaitDuration() == Duration.ofSeconds(10)
        dataSource.getLoginTimeout() == 20

        when:
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("person")))

        then:
        dataSource.getInitialPoolSize() == 5
        dataSource.getMinPoolSize() == 5
        dataSource.getMaxPoolSize() == 20
        dataSource.getTimeoutCheckInterval() == 5
        dataSource.getInactiveConnectionTimeout() == 10
        dataSource.getConnectionWaitDuration() == Duration.ofSeconds(10)
        dataSource.getLoginTimeout() == 20

        cleanup:
        applicationContext.close()
    }

    void "test multiple datasources metadata props"() {
        given:
        String context = UUID.randomUUID().toString()
        ApplicationContext applicationContext = new DefaultApplicationContext(context)
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                context,
                [
                        'datasources.default.initialPoolSize'          : 5,
                        'datasources.default.minPoolSize'              : 5,
                        'datasources.default.maxPoolSize'              : 20,

                        'datasources.person.initialPoolSize'           : 1,
                        'datasources.person.minPoolSize'               : 1,
                        'datasources.person.maxPoolSize'               : 10,
                ]
        ))
        applicationContext.start()

        def metadataDefault = applicationContext.getBean(OracleUcpDataSourcePoolMetadata, Qualifiers.byName("default"))
        def metadataPerson = applicationContext.getBean(OracleUcpDataSourcePoolMetadata, Qualifiers.byName("person"))

        expect:
        verifyAll {
            applicationContext.getBeansOfType(DataSource).size() == 2
            applicationContext.getBeansOfType(DatasourceConfiguration).size() == 2

            metadataDefault.max == 20
            metadataDefault.min == 5
            metadataDefault.active == 0
            metadataDefault.idle >= 0

            metadataPerson.max == 10
            metadataPerson.min == 1
            metadataPerson.active == 0
            metadataPerson.idle >= 0
        }

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
                        'datasources.person.connectionWaitDuration'    : "10s",
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
                ["datasources.default.data-source-properties": ["oracle.fan.enabled": true],
                 "oracle.ucp.createConnectionInBorrowThread": "true"]
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
        Util.createConnectionInBorrowThread()
        System.setProperty("oracle.ucp.createConnectionInBorrowThread", "false")
        !Util.createConnectionInBorrowThread()
    }
}
