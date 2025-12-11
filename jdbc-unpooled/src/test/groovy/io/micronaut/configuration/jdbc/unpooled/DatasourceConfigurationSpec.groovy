/*
 * Copyright 2017-2025 original authors
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
package io.micronaut.configuration.jdbc.unpooled

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(UnpooledDataSource)
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
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        UnpooledDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        when: "Get a connection and verify it works"
        Connection conn = dataSource.getConnection()
        ResultSet rs = conn.prepareStatement("SELECT 1").executeQuery()

        then:
        rs.next()
        rs.getInt(1) == 1

        cleanup:
        rs?.close()
        conn?.close()
        applicationContext.close()
    }

    void "test connections are truly unpooled"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        when:
        DataSource dataSource = applicationContext.getBean(DataSource)
        Connection conn1 = dataSource.getConnection()
        Connection conn2 = dataSource.getConnection()

        then: "Each connection is a different object (not pooled)"
        conn1 != conn2
        !conn1.is(conn2)

        cleanup:
        conn1?.close()
        conn2?.close()
        applicationContext.close()
    }

    void "test connection is actually closed"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        when:
        DataSource dataSource = applicationContext.getBean(DataSource)
        Connection conn = dataSource.getConnection()

        then:
        !conn.isClosed()

        when:
        conn.close()

        then:
        conn.isClosed()

        cleanup:
        applicationContext.close()
    }

    void "test datasource can be disabled"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                [
                        'datasources.default': [:],
                        'datasources.default.enabled': false
                ]
        ))
        applicationContext.start()

        when:
        applicationContext.getBean(DataSource)

        then:
        thrown(NoSuchBeanException)

        when:
        applicationContext.getBean(UnpooledDataSource)

        then:
        thrown(NoSuchBeanException)

        cleanup:
        applicationContext.close()
    }

    void "test credential refresh"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        System.setProperty("ds-default-password", "")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.password': '${ds-default-password}',
                 'datasources.default.url': 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.username': 'sa',
                 'datasources.default.driver-class-name': 'org.h2.Driver']
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        UnpooledDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource

        then:
        dataSource.password == ''

        when: "Fire datasource password change event"
        def newPassword = 'new_pwd'
        System.setProperty("ds-default-password", newPassword)
        def changes = applicationContext.environment.refreshAndDiff()
        def conn = dataSource.getConnection()
        conn.prepareStatement("ALTER USER sa SET PASSWORD '" + newPassword + "'").executeUpdate()
        conn.close()
        applicationContext.publishEvent(new RefreshEvent(changes))
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource

        then: "Password is updated and new connections use it"
        dataSource.password == newPassword
        def newConn = dataSource.getConnection()
        def newRs = newConn.prepareStatement("SELECT 1").executeQuery()
        newRs.next()
        newRs.getInt(1) == 1

        cleanup:
        newRs?.close()
        newConn?.close()
        // Change back to default password
        def resetConn = dataSource.getConnection()
        resetConn.prepareStatement("ALTER USER sa SET PASSWORD ''").executeUpdate()
        resetConn.close()
        System.setProperty("ds-default-password", "")
        applicationContext.close()
    }

    void "test multiple data sources are configured"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.foo': [:]]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        UnpooledDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        when:
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("foo"))) as UnpooledDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }

    void "test properties are bindable"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.url': 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.login-timeout': 30,
                 'datasources.default.data-source-properties.prop1': 'value1',
                 'datasources.default.data-source-properties.prop2': 'value2']
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        UnpooledDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource

        then:
        dataSource.url == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.loginTimeout == 30

        cleanup:
        applicationContext.close()
    }

    void "test operations with a blank connection"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        UnpooledDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource
        Connection conn = dataSource.getConnection()
        ResultSet resultSet = conn.prepareStatement("SELECT H2VERSION() FROM DUAL").executeQuery()
        resultSet.next()
        String version = resultSet.getString(1)

        then:
        version == '2.4.240'

        cleanup:
        resultSet?.close()
        conn?.close()
        applicationContext.close()
    }
}
