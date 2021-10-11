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
import io.micronaut.configuration.metrics.binder.datasource.DataSourcePoolMetricsBinderFactory
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.metadata.DataSourcePoolMetadata
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.ResultSet

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(HikariDataSource)
        !applicationContext.containsBean(DatasourceConfiguration)
        !applicationContext.containsBean(DataSourcePoolMetadata)

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
        HikariUrlDataSource dataSource = applicationContext.getBean(DataSource).targetDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.jdbcUrl == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

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

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(DataSource).targetDataSource
        ResultSet resultSet = dataSource.getConnection().prepareStatement("SELECT H2VERSION() FROM DUAL").executeQuery()
        resultSet.next()
        String version = resultSet.getString(1)

        then:
        version == '1.4.200'

        cleanup:
        applicationContext.close()
    }

    void "test properties are bindable"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.connectionTimeout'        : 500,
                 'datasources.default.idleTimeout'              : 20000,
                 'datasources.default.catalog'                  : 'foo',
                 'datasources.default.autoCommit'               : true,
                 'datasources.default.healthCheckProperties.foo': 'bar',
                 'datasources.default.jndiName'                 : 'java:comp/env/FooBarPool',
                 'datasources.default.url'                      : 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.validationQuery'          : 'select 3']
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(DataSource).targetDataSource

        then:
        dataSource.connectionTimeout == 500
        dataSource.idleTimeout == 20000
        dataSource.catalog == 'foo'
        dataSource.autoCommit
        dataSource.healthCheckProperties.getProperty('foo') == 'bar'
        dataSource.dataSourceJNDI == 'java:comp/env/FooBarPool'
        dataSource.jdbcUrl == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.connectionTestQuery == 'select 3'

        cleanup:
        applicationContext.close()
    }

    void "test multiple data sources are configured"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.foo'    : [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(DataSource).targetDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.jdbcUrl == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        when:
        dataSource = applicationContext.getBean(DataSource, Qualifiers.byName("foo")).targetDataSource

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.jdbcUrl == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }

    void "test multiple datasources are all wired"() {
        given:
        HikariDataSource dataSource
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:],
                 'datasources.foo'    : [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.getBeansOfType(DataSource).size() == 2
        applicationContext.getBeansOfType(DatasourceConfiguration).size() == 2

        when:
        dataSource = (HikariDataSource) applicationContext.getBean(DataSource, Qualifiers.byName("default")).targetDataSource

        then:
        dataSource.jdbcUrl == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        when:
        dataSource = (HikariDataSource) applicationContext.getBean(DataSource, Qualifiers.byName("foo")).targetDataSource

        then:
        dataSource.jdbcUrl == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }

    void "test metrics disabled"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default'      : [:],
                 'datasources.foo'          : [:],
                 (MICRONAUT_METRICS_ENABLED): false
                ]
        ))

        when:
        applicationContext.start()

        then:
        !applicationContext.containsBean(DataSourcePoolMetricsBinderFactory)

        cleanup:
        applicationContext.stop()
    }

    void "test jdbc metrics disabled"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default'                        : [:],
                 'datasources.foo'                            : [:],
                 (MICRONAUT_METRICS_ENABLED)                  : true,
                 (MICRONAUT_METRICS_BINDERS + ".jdbc.enabled"): false,
                ]
        ))

        when:
        applicationContext.start()

        then:
        !applicationContext.containsBean(DataSourcePoolMetricsBinderFactory)

        cleanup:
        applicationContext.stop()
    }

    void "test data source properties"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.data-source-properties' : ['reWriteBatchInserts' : true, 'anotherOne' : 'value']]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        HikariDataSource dataSource = applicationContext.getBean(DataSource).targetDataSource

        then:
        !dataSource.dataSourceProperties.isEmpty()
        dataSource.dataSourceProperties.size() == 2
        dataSource.dataSourceProperties.get('reWriteBatchInserts') == true
        dataSource.dataSourceProperties.get('anotherOne') == 'value'

        cleanup:
        applicationContext.close()
    }

}
