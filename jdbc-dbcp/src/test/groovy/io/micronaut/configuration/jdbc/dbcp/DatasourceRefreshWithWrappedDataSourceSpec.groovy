package io.micronaut.configuration.jdbc.dbcp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import org.apache.commons.dbcp2.BasicDataSource
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection

class DatasourceRefreshWithWrappedDataSourceSpec extends Specification {

    private static final String URL = "jdbc:h2:mem:dbcpWrappedRefresh;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"

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
                        "datasources.default.url": URL,
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
        dataSource.url == URL
        dataSource.username == "sa"
        dataSource.password == ""
        dataSource.driverClassName == "org.h2.Driver"
        dataSource.validationQuery == "SELECT 1"
        selectOne(dataSource) == 1

        when:
        def newPassword = "wrapped_pwd"
        dataSource.connection.withCloseable { Connection connection ->
            connection.prepareStatement("ALTER USER sa SET PASSWORD '" + newPassword + "'").executeUpdate()
        }
        // hold a connection created with the old password so the pool cannot hand out a pre-authenticated one
        Connection oldConnection = dataSource.connection
        System.setProperty("ds-default-password", newPassword)
        def changes = applicationContext.environment.refreshAndDiff()
        applicationContext.publishEvent(new RefreshEvent(changes))
        dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as BasicDataSource

        then: "the pool was restarted and new connections authenticate with the new password"
        dataSource.password == newPassword
        dataSource.numActive == 0
        dataSource.numIdle == 0
        selectOne(dataSource) == 1

        cleanup:
        try {
            oldConnection?.close()
            dataSource?.connection?.withCloseable { Connection connection ->
                connection.prepareStatement("ALTER USER sa SET PASSWORD ''").executeUpdate()
            }
        } finally {
            System.setProperty("ds-default-password", "")
            if (applicationContext?.isRunning()) {
                def revertedChanges = applicationContext.environment.refreshAndDiff()
                applicationContext.publishEvent(new RefreshEvent(revertedChanges))
                applicationContext.close()
            }
        }
    }

    void "test password change with full refresh event"() {
        given:
        System.setProperty("ds-full-refresh-password", "")
        ApplicationContext applicationContext = ApplicationContext.run([
                "spec.name": "DatasourceRefreshWithWrappedDataSourceSpec",
                "datasources.default.password": '${ds-full-refresh-password}',
                "datasources.default.url": "jdbc:h2:mem:dbcpFullRefresh;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                "datasources.default.username": "sa",
                "datasources.default.driver-class-name": "org.h2.Driver",
                "datasources.default.validation-query": "SELECT 1"
        ], "test")
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)
        BasicDataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource)) as BasicDataSource

        when:
        def newPassword = "full_refresh_pwd"
        dataSource.connection.withCloseable { Connection connection ->
            connection.prepareStatement("ALTER USER sa SET PASSWORD '" + newPassword + "'").executeUpdate()
        }
        // hold a connection created with the old password so the pool cannot hand out a pre-authenticated one
        Connection oldConnection = dataSource.connection
        System.setProperty("ds-full-refresh-password", newPassword)
        applicationContext.environment.refresh()
        applicationContext.publishEvent(new RefreshEvent())

        then: "the pool was restarted and new connections authenticate with the new password"
        dataSource.password == newPassword
        dataSource.numActive == 0
        selectOne(dataSource) == 1

        cleanup:
        try {
            oldConnection?.close()
            dataSource?.connection?.withCloseable { Connection connection ->
                connection.prepareStatement("ALTER USER sa SET PASSWORD ''").executeUpdate()
            }
        } finally {
            System.clearProperty("ds-full-refresh-password")
            applicationContext?.close()
        }
    }

    private static int selectOne(BasicDataSource dataSource) {
        dataSource.connection.withCloseable { Connection connection ->
            def rs = connection.prepareStatement(dataSource.validationQuery).executeQuery()
            rs.next()
            rs.getInt(1)
        }
    }
}
