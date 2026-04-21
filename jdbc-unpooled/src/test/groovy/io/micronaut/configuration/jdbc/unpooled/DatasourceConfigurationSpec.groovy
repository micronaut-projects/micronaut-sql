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
package io.micronaut.configuration.jdbc.unpooled

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.SQLException

class DatasourceConfigurationSpec extends Specification {

    void "test blank configuration creates unpooled datasource"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        when:
        DriverManagerDataSource dataSource = applicationContext.getBean(DataSource) as DriverManagerDataSource
        def connection1 = dataSource.connection
        def connection2 = dataSource.connection

        then:
        connection1 != connection2
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'
        connection1.prepareStatement('SELECT 1').executeQuery().withCloseable { rs ->
            rs.next()
            rs.getInt(1) == 1
        }

        when:
        connection1.close()

        then:
        connection1.isClosed()
        !connection2.isClosed()

        cleanup:
        connection2?.close()
        applicationContext.close()
    }

    void "test datasource password change updates new connections"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        System.setProperty("ds-default-password", "")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.password': '${ds-default-password}',
                 'datasources.default.dialect': 'H2',
                 'datasources.default.url': 'jdbc:h2:mem:default-password;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.username': 'sa',
                 'datasources.default.driver-class-name': 'org.h2.Driver']
        ))
        applicationContext.start()

        when:
        DriverManagerDataSource dataSource = applicationContext.getBean(DataSource) as DriverManagerDataSource
        def connection = dataSource.connection
        connection.prepareStatement("ALTER USER sa SET PASSWORD 'new_pwd'").executeUpdate()
        connection.close()

        System.setProperty("ds-default-password", "new_pwd")
        def changes = applicationContext.environment.refreshAndDiff()
        applicationContext.publishEvent(new RefreshEvent(changes))

        then:
        dataSource.password == 'new_pwd'
        dataSource.connection.withCloseable { refreshed ->
            refreshed.prepareStatement('SELECT 1').executeQuery().withCloseable { rs ->
                rs.next()
                rs.getInt(1) == 1
            }
        }

        cleanup:
        try {
            dataSource?.connection?.withCloseable { it.prepareStatement("ALTER USER sa SET PASSWORD ''").executeUpdate() }
        } catch (SQLException ignored) {
        }
        System.setProperty("ds-default-password", "")
        if (applicationContext?.isRunning()) {
            def revertedChanges = applicationContext.environment.refreshAndDiff()
            applicationContext.publishEvent(new RefreshEvent(revertedChanges))
            applicationContext.close()
        }
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
                        'datasources.default.enabled': false,
                        'datasources.custom': [:]
                ]
        ))
        applicationContext.start()

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('default'))

        then:
        thrown(NoSuchBeanException)

        when:
        DriverManagerDataSource dataSource = applicationContext.getBean(DataSource, Qualifiers.byName('custom')) as DriverManagerDataSource

        then:
        dataSource.url == 'jdbc:h2:mem:custom;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
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
                 'datasources.default.driver-class-name': 'org.h2.Driver',
                 'datasources.default.username': 'user',
                 'datasources.default.password': 'secret',
                 'datasources.default.login-timeout': 7,
                 'datasources.default.data-source-properties.traceLevelFile': '3']
        ))
        applicationContext.start()

        when:
        DriverManagerDataSource dataSource = applicationContext.getBean(DataSource) as DriverManagerDataSource

        then:
        dataSource.url == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.driverClassName == 'org.h2.Driver'
        dataSource.username == 'user'
        dataSource.password == 'secret'
        dataSource.loginTimeout == 7
        dataSource.dataSourceProperties.traceLevelFile == '3'

        cleanup:
        applicationContext.close()
    }

    void "test driver class property is bindable"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.url': 'jdbc:h2:mem:typed-driver;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.driver-class': 'org.h2.Driver',
                 'datasources.default.username': 'sa',
                 'datasources.default.password': '']
        ))
        applicationContext.start()

        when:
        DatasourceConfiguration configuration = applicationContext.getBean(DatasourceConfiguration)
        DriverManagerDataSource dataSource = applicationContext.getBean(DataSource) as DriverManagerDataSource

        then:
        configuration.driverClass.name == 'org.h2.Driver'
        configuration.configuredDriverClassName == 'org.h2.Driver'
        dataSource.driverClassName == 'org.h2.Driver'
        dataSource.connection.withCloseable { connection ->
            connection.prepareStatement('SELECT 1').executeQuery().withCloseable { rs ->
                rs.next()
                rs.getInt(1) == 1
            }
        }

        cleanup:
        applicationContext.close()
    }
}
