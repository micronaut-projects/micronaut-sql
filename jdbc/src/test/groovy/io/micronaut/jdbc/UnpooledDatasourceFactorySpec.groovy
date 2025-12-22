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
package io.micronaut.jdbc

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.ResultSet

class UnpooledDatasourceFactorySpec extends Specification {

    void "test no configuration - unpooled datasource not created"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created when allow-unpooled is not set"
        !applicationContext.containsBean(UnpooledDataSource)
        !applicationContext.containsBean(UnpooledDatasourceConfiguration)

        cleanup:
        applicationContext.close()
    }

    void "test datasource not created without allow-unpooled flag"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        when:
        applicationContext.getBean(DataSource)

        then:
        thrown(NoSuchBeanException)

        cleanup:
        applicationContext.close()
    }

    void "test datasource created with global allow-unpooled flag"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.allow-unpooled': true]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(UnpooledDatasourceConfiguration)

        when:
        UnpooledDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource

        then: "The default configuration is supplied because H2 is on the classpath"
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

    void "test datasource created with per-datasource allow-unpooled flag"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.default.allow-unpooled': true]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(UnpooledDatasourceConfiguration)

        when:
        UnpooledDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as UnpooledDataSource

        then: "The default configuration is supplied because H2 is on the classpath"
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }

    void "test per-datasource flag overrides global flag"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.default.allow-unpooled': false,
                 'datasources.allow-unpooled': true]
        ))
        applicationContext.start()

        when:
        applicationContext.getBean(DataSource)

        then: "Per-datasource flag set to false overrides global flag"
        thrown(NoSuchBeanException)

        cleanup:
        applicationContext.close()
    }

    void "test multiple datasources with mixed flags"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.default.allow-unpooled': true,
                 'datasources.foo': [:],
                 'datasources.foo.allow-unpooled': false]
        ))
        applicationContext.start()

        expect: "Default datasource is created"
        applicationContext.containsBean(DataSource)

        when: "Try to get the 'foo' datasource"
        applicationContext.getBean(DataSource, Qualifiers.byName("foo"))

        then: "Foo datasource is not created because allow-unpooled is false"
        thrown(NoSuchBeanException)

        cleanup:
        applicationContext.close()
    }

    void "test connections are truly unpooled"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.allow-unpooled': true]
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
                ['datasources.default': [:],
                 'datasources.allow-unpooled': true]
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
}
