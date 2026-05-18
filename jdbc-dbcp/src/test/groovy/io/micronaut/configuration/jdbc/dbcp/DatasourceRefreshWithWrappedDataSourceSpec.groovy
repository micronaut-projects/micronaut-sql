package io.micronaut.configuration.jdbc.dbcp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import org.apache.commons.dbcp2.BasicDataSource
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceRefreshWithWrappedDataSourceSpec extends Specification {

    void "test default configuration and password change with wrapped datasource"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        System.setProperty("ds-default-password", "")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                "test",
                [
                        "spec.name": "DatasourceRefreshWithWrappedDataSourceSpec",
                        "datasources.default.password": '${ds-default-password}',
                        "datasources.default.dialect": "H2",
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.driver-class-name": "org.h2.Driver",
                        "datasources.default.validation-query": "SELECT 1"
                ]
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        BasicDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as BasicDataSource

        then:
        dataSource.url == "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        dataSource.username == "sa"
        dataSource.password == ""
        dataSource.driverClassName == "org.h2.Driver"
        dataSource.validationQuery == "SELECT 1"
        def rs = dataSource.connection.prepareStatement(dataSource.validationQuery).executeQuery()
        rs.next()
        rs.getInt(1) == 1

        when:
        def newPassword = "wrapped_pwd"
        System.setProperty("ds-default-password", newPassword)
        def changes = applicationContext.environment.refreshAndDiff()
        dataSource.connection.prepareStatement("ALTER USER sa SET PASSWORD '" + newPassword + "'").executeUpdate()
        applicationContext.publishEvent(new RefreshEvent(changes))
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as BasicDataSource

        then:
        dataSource.password == newPassword
        def newRs = dataSource.connection.prepareStatement(dataSource.validationQuery).executeQuery()
        newRs.next()
        newRs.getInt(1) == 1

        cleanup:
        try {
            dataSource?.connection?.prepareStatement("ALTER USER sa SET PASSWORD ''")?.executeUpdate()
        } finally {
            System.setProperty("ds-default-password", "")
            if (applicationContext?.isRunning()) {
                def revertedChanges = applicationContext.environment.refreshAndDiff()
                applicationContext.publishEvent(new RefreshEvent(revertedChanges))
                applicationContext.close()
            }
        }
    }
}
