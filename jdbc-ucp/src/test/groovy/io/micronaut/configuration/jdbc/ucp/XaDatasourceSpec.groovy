package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolXADataSource
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import javax.sql.XAConnection
import javax.sql.XADataSource

class XaDatasourceSpec extends Specification {

    void "test an XA pool is created when xa is enabled"() {
        given:
        ApplicationContext context = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:xaDefault;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.default.connection-factory-class-name': 'org.h2.jdbcx.JdbcDataSource',
            'datasources.default.xa': true
        )

        when:
        PoolDataSource pool = context.getBean(DatasourceConfiguration).poolDataSource
        XADataSource xaDataSource = context.getBean(XADataSource)

        then:
        pool instanceof PoolXADataSource
        ((UcpXADataSource) xaDataSource).pool().is(pool)
        context.getBeansOfType(DataSource).size() == 1

        when:
        XAConnection xaConnection = xaDataSource.getXAConnection()
        def resultSet = xaConnection.connection.createStatement().executeQuery("SELECT 1")

        then:
        resultSet.next()
        resultSet.getInt(1) == 1
        xaConnection.XAResource != null

        cleanup:
        xaConnection?.close()
        context?.close()
    }

    void "test the XA pool of a named datasource configured with driver-class-name"() {
        given:
        ApplicationContext context = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:plainDefault;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.orders.url': 'jdbc:h2:mem:xaOrders;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.orders.username': 'sa',
            'datasources.orders.password': '',
            'datasources.orders.driver-class-name': 'org.h2.jdbcx.JdbcDataSource',
            'datasources.orders.xa': true
        )

        expect:
        !context.getBean(DatasourceConfiguration).xa
        context.getBean(DatasourceConfiguration, Qualifiers.byName("orders")).poolDataSource instanceof PoolXADataSource
        ((UcpXADataSource) context.getBean(XADataSource, Qualifiers.byName("orders"))).pool().is(context.getBean(DatasourceConfiguration, Qualifiers.byName("orders")).poolDataSource)
        !context.findBean(XADataSource, Qualifiers.byName("default")).present
        context.getBeansOfType(XADataSource).size() == 1

        cleanup:
        context?.close()
    }

    void "test the DataSource bean of an XA pool opens local connections and returns them to the pool"() {
        given:
        ApplicationContext context = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:xaLocal;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.default.connection-factory-class-name': 'org.h2.jdbcx.JdbcDataSource',
            'datasources.default.xa': true
        )
        DataSource dataSource = context.getBean(DataSource)
        PoolDataSource pool = context.getBean(DatasourceConfiguration).poolDataSource

        when: "a local transaction runs on a connection of the DataSource bean"
        Connection connection = dataSource.connection
        connection.autoCommit = false
        connection.createStatement().execute("CREATE TABLE note (id INT PRIMARY KEY, text VARCHAR(100))")
        // after the first statement, since the bean may be a proxy that borrows lazily
        int borrowedWhileOpen = pool.borrowedConnectionsCount
        connection.createStatement().executeUpdate("INSERT INTO note VALUES (1, 'local')")
        connection.commit()
        connection.close()

        then: "the connection came from the pool and went back to it"
        borrowedWhileOpen == 1
        pool.borrowedConnectionsCount == 0

        when:
        Connection other = dataSource.connection
        def resultSet = other.createStatement().executeQuery("SELECT text FROM note WHERE id = 1")

        then:
        resultSet.next()
        resultSet.getString(1) == 'local'

        cleanup:
        other?.close()
        context?.close()
    }

    void "test an XA connection factory without xa keeps a plain pool"() {
        given:
        ApplicationContext context = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:xaOff;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.default.connection-factory-class-name': 'org.h2.jdbcx.JdbcDataSource'
        )

        expect:
        !context.getBean(DatasourceConfiguration).xa
        !(context.getBean(DatasourceConfiguration).poolDataSource instanceof PoolXADataSource)
        !context.findBean(XADataSource).present

        cleanup:
        context?.close()
    }

    void "test xa without an XA connection factory fails"() {
        when:
        ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:xaWrong;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.default.xa': true
        ).close()

        then:
        Exception e = thrown()
        (e.message + e.cause?.message).contains("has xa enabled, but its connection factory class [null] is not an XADataSource")
    }

    void "test no XA pool without an XA connection factory"() {
        given:
        ApplicationContext context = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:noXa;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': ''
        )

        expect:
        !(context.getBean(DatasourceConfiguration).poolDataSource instanceof PoolXADataSource)
        !context.findBean(XADataSource).present

        cleanup:
        context?.close()
    }
}
