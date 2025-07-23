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

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.annotation.Context
import io.micronaut.context.annotation.EachBean
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Primary
import io.micronaut.context.annotation.Replaces
import io.micronaut.context.annotation.Requires
import io.micronaut.context.env.MapPropertySource
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException
import java.sql.SQLFeatureNotSupportedException
import java.util.logging.Logger


class CustomDatasourceConfigurationSpec extends Specification {

    void "test default configuration and password change"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        System.setProperty("ds-default-password", "")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.password'             : '${ds-default-password}',
                 'datasources.default.dialect'              : 'H2',
                 'datasources.default.url'                  : 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.username'             : 'sa',
                 'datasources.default.driver-class-name'    : 'org.h2.Driver',
                 'datasources.default.connection-test-query': 'SELECT 1',
                 'use-custom-factory'                       : 'true']
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        TracingDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.jdbcUrl == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'
        dataSource.connectionTestQuery == "SELECT 1"
        def rs = dataSource.connection.prepareStatement(dataSource.connectionTestQuery).executeQuery()
        rs.next()
        rs.getInt(1) == 1

        when: "Fire datasource password change event"
        def newPassword = 'new_pwd'
        System.setProperty("ds-default-password", newPassword)
        def changes = applicationContext.environment.refreshAndDiff()
        dataSource.connection.prepareStatement("ALTER USER sa SET PASSWORD '" + newPassword + "'").executeUpdate()
        applicationContext.publishEvent(new RefreshEvent(changes))
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: "Password is updated"
        dataSource.password == newPassword
        def newRs = dataSource.connection.prepareStatement(dataSource.connectionTestQuery).executeQuery()
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




}
