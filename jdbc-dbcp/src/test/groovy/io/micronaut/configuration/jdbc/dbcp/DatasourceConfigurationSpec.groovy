package io.micronaut.configuration.jdbc.dbcp

import io.micronaut.context.exceptions.NoSuchBeanException
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import org.apache.commons.dbcp2.BasicDataSource
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.inject.qualifiers.Qualifiers
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.ResultSet

class DatasourceConfigurationSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(BasicDataSource)
        !applicationContext.containsBean(DatasourceConfiguration)

        cleanup:
        applicationContext.close()
    }

    void "test blank configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                "test",
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(BasicDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        BasicDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'
        dataSource.validationQuery == 'SELECT 1'
        def rs = dataSource.connection.prepareStatement(dataSource.validationQuery).executeQuery()
        rs.next()
        rs.getInt(1) == 1

        cleanup:
        applicationContext.close()
    }

    void "test default configuration and password change"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        System.setProperty("ds-default-password", "")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                "test",
                ['datasources.default.password': '${ds-default-password}',
                 'datasources.default.dialect': 'H2',
                 'datasources.default.url': 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.username': 'sa',
                 'datasources.default.driver-class-name': 'org.h2.Driver',
                 'datasources.default.validation-query': 'SELECT 1']
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(BasicDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        BasicDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'
        dataSource.validationQuery == 'SELECT 1'
        def rs = dataSource.connection.prepareStatement(dataSource.validationQuery).executeQuery()
        rs.next()
        rs.getInt(1) == 1

        when:"Fire datasource password change event"
        def newPassword = "changed_pwd"
        System.setProperty("ds-default-password", newPassword)
        def changes = applicationContext.environment.refreshAndDiff()
        dataSource.connection.prepareStatement("ALTER USER sa SET PASSWORD '" + newPassword + "'").executeUpdate()
        applicationContext.publishEvent(new RefreshEvent(changes))
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then:"Password is updated"
        dataSource.password == newPassword
        def newRs = dataSource.connection.prepareStatement(dataSource.validationQuery).executeQuery()
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
                "test",
                [
                        'datasources.default': [:],
                        'datasources.default.enabled': false
                ]
        ))
        applicationContext.start()

        when:
        applicationContext.getBean(DatasourceConfiguration)
        then:
        def exception = thrown(NoSuchBeanException)
        exception.message.contains('The datasource "default" is disabled')
        when:
        applicationContext.getBean(DataSource)
        then:
        exception = thrown(NoSuchBeanException)
        exception.message.contains('The datasource "default" is disabled')
        when:
        applicationContext.getBean(BasicDataSource)
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
        applicationContext.getBean(DatasourceConfiguration, Qualifiers.byName('default'))
        then:
        thrown(NoSuchBeanException)

        when:
        applicationContext.getBean(DataSource, Qualifiers.byName('default'))
        then:
        thrown(NoSuchBeanException)
        when:
        applicationContext.getBean(BasicDataSource, Qualifiers.byName('default'))
        then:
        thrown(NoSuchBeanException)

        when:
        DataSource customDataSource = applicationContext.getBean(DataSource, Qualifiers.byName('custom'))
        then:
        noExceptionThrown()
        customDataSource

        when:
        BasicDataSource dataSource = dataSourceResolver.resolve(customDataSource)

        then: //The configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:custom;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'
        dataSource.validationQuery == 'SELECT 1'

        cleanup:
        applicationContext.close()
    }

    void "test operations with a blank connection"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                "test",
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(BasicDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        BasicDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as BasicDataSource
        ResultSet resultSet = dataSource.getConnection().prepareStatement("SELECT H2VERSION() FROM DUAL").executeQuery()
        resultSet.next()
        String version = resultSet.getString(1)

        then:
        version == '2.4.240'

        cleanup:
        applicationContext.close()
    }

    void "test properties are bindable"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                "test",
                ['datasources.default.maxWaitMillis': 5000,
                'datasources.default.connectionProperties': 'prop1=value1;prop2=value2',
                'datasources.default.defaultAutoCommit': true,
                'datasources.default.defaultCatalog': 'catalog']
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(BasicDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        BasicDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then:
        dataSource.maxWaitMillis == 5000
        dataSource.defaultAutoCommit
        dataSource.defaultCatalog == 'catalog'

        cleanup:
        applicationContext.close()
    }

    void "test multiple data sources are configured"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                "test",
                ['datasources.default': [:],
                'datasources.foo': [:]]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver =  applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(BasicDataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        BasicDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource, Qualifiers.byName("foo")))

        then: //The default configuration is supplied because H2 is on the classpath
        dataSource.url == 'jdbc:h2:mem:foo;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
        dataSource.username == 'sa'
        dataSource.password == ''
        dataSource.driverClassName == 'org.h2.Driver'

        cleanup:
        applicationContext.close()
    }

}
