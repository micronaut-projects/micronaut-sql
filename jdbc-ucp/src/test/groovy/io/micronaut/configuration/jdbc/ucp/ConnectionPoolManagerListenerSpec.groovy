package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.jdbc.DataSourceResolver
import oracle.ucp.admin.UniversalConnectionPoolManager
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Specification

import javax.sql.DataSource


class ConnectionPoolManagerListenerSpec extends Specification {

    void "test ucp-manager is enabled by default but does not manage any pools"() {
        given:
        var applicationContext = ApplicationContext.run("test")

        expect:
        !applicationContext.containsBean(DataSource)
        applicationContext.containsBean(ConnectionPoolManagerListener)
        applicationContext.containsBean(UniversalConnectionPoolManager)

        when:
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)

        then:
        poolManager.getConnectionPoolNames() == new String[0]

        cleanup:
        applicationContext.close()
    }

    void "test ucp-manager is enabled by default and managing default pool"() {
        given:
        var applicationContext = ApplicationContext.run(
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": ""],
                "test")

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(ConnectionPoolManagerListener)
        applicationContext.containsBean(UniversalConnectionPoolManager)

        when:
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)

        then:
        poolManager.getConnectionPoolNames() == new String[]{"default"}

        cleanup:
        applicationContext.close()
    }

    void "test ucp-manager is enabled by default and managing default pool with custom name"() {
        given:
        var applicationContext = ApplicationContext.run(
                [
                        "datasources.default.url": "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                        "datasources.default.username": "sa",
                        "datasources.default.password": "",
                        "datasources.default.connectionPoolName": "JDBC_UCP"],
                "test")

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(ConnectionPoolManagerListener)
        applicationContext.containsBean(UniversalConnectionPoolManager)

        when:
        var poolManager = applicationContext.getBean(UniversalConnectionPoolManager)

        then:
        poolManager.getConnectionPoolNames() == new String[]{"JDBC_UCP"}

        cleanup:
        applicationContext.close()
    }

    void "test disabled ucp-manager"() {
        given:
        var applicationContext = ApplicationContext.run(
                [
                        "ucp-manager.enabled": false,
                ],
                "test")

        expect:
        !applicationContext.containsBean(ConnectionPoolManagerListener)
        !applicationContext.containsBean(UniversalConnectionPoolManager)

        cleanup:
        applicationContext.close()
    }
}
