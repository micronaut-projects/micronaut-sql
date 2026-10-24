package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolXADataSource
import spock.lang.Specification

import javax.sql.DataSource
import javax.sql.XAConnection
import javax.sql.XADataSource

class XaDatasourceSpec extends Specification {

    void "test an XA pool is created when the connection factory is an XADataSource"() {
        given:
        ApplicationContext context = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:xaDefault;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.default.connection-factory-class-name': 'org.h2.jdbcx.JdbcDataSource'
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
            'datasources.orders.driver-class-name': 'org.h2.jdbcx.JdbcDataSource'
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
