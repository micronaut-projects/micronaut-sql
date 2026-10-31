package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.ConfigurationException
import io.micronaut.inject.qualifiers.Qualifiers
import oracle.ucp.ConnectionLabelingCallback
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolXADataSource
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import javax.sql.XAConnection
import javax.sql.XADataSource
import java.io.PrintWriter
import java.io.StringWriter

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

    void "test closing a local connection returns its XA connection without sharing sessions"() {
        given:
        ApplicationContext context = xaContext('xaSessions')
        DataSource dataSource = context.getBean(DatasourceConfiguration).poolDataSource
        XADataSource xaDataSource = context.getBean(XADataSource)
        PoolDataSource pool = context.getBean(DatasourceConfiguration).poolDataSource

        when: "a local connection is closed, then two XA connections are held at once"
        dataSource.connection.close()
        XAConnection first = xaDataSource.getXAConnection()
        XAConnection second = xaDataSource.getXAConnection()

        then: "both are borrowed, each with its own session"
        pool.borrowedConnectionsCount == 2
        sessionId(first.connection) != sessionId(second.connection)

        cleanup:
        first?.close()
        second?.close()
        context?.close()
    }

    void "test every getConnection variant and the connection builder of an XA pool return to the pool"() {
        given:
        ApplicationContext context = xaContext('xaVariants')
        PoolDataSource pool = context.getBean(DatasourceConfiguration).poolDataSource
        // labeled borrows need a labeling callback
        pool.registerConnectionLabelingCallback(new ConnectionLabelingCallback() {
            @Override
            int cost(Properties requestedLabels, Properties currentLabels) {
                return 0
            }

            @Override
            boolean configure(Properties requestedLabels, Object labeledConnection) {
                return true
            }
        })

        when:
        Connection connection = open(pool)
        int borrowed = pool.borrowedConnectionsCount
        def resultSet = connection.createStatement().executeQuery("SELECT 1")

        then:
        borrowed == 1
        resultSet.next()
        !connection.closed

        when:
        connection.close()
        connection.close()

        then: "closing twice returns the XA connection once"
        connection.closed
        pool.borrowedConnectionsCount == 0

        cleanup:
        context?.close()

        where:
        open << [
            { PoolDataSource p -> p.getConnection('sa', '') },
            { PoolDataSource p -> p.getConnection(new Properties()) },
            { PoolDataSource p -> p.getConnection('sa', '', new Properties()) },
            { PoolDataSource p -> p.createConnectionBuilder().user('sa').password('').labels(new Properties()).build() },
            { PoolDataSource p -> p.createConnectionBuilder().connectionWaitTimeout(5).build() },
            { PoolDataSource p -> p.createConnectionBuilder().connectionWaitDuration(java.time.Duration.ofSeconds(5)).build() }
        ]
    }

    void "test a local connection of an XA pool unwraps to the driver connection"() {
        given:
        ApplicationContext context = xaContext('xaUnwrap')
        Connection connection = context.getBean(DatasourceConfiguration).poolDataSource.connection
        Class<?> h2Connection = Class.forName("org.h2.jdbc.JdbcConnection")

        expect:
        connection instanceof XALocalConnection
        connection.isWrapperFor(XALocalConnection)
        connection.unwrap(XALocalConnection).is(connection)
        connection.isWrapperFor(h2Connection)

        cleanup:
        connection?.close()
        context?.close()
    }

    void "test the XADataSource bean delegates to the pool"() {
        given:
        ApplicationContext context = xaContext('xaDelegate')
        XADataSource xaDataSource = context.getBean(XADataSource)
        PoolDataSource pool = context.getBean(DatasourceConfiguration).poolDataSource
        PrintWriter writer = new PrintWriter(new StringWriter())

        when:
        xaDataSource.logWriter = writer
        xaDataSource.loginTimeout = 7
        XAConnection withCredentials = xaDataSource.getXAConnection('sa', '')
        XAConnection built = xaDataSource.createXAConnectionBuilder().user('sa').password('').build()

        then:
        pool.logWriter.is(writer)
        xaDataSource.logWriter.is(writer)
        xaDataSource.loginTimeout == pool.loginTimeout
        xaDataSource.parentLogger != null
        pool.borrowedConnectionsCount == 2

        cleanup:
        withCredentials?.close()
        built?.close()
        context?.close()
    }

    void "test xa cannot be switched on a configuration that is bound"() {
        given:
        ApplicationContext context = xaContext('xaSwitch')
        DatasourceConfiguration configuration = context.getBean(DatasourceConfiguration)

        when:
        configuration.xa = true

        then:
        noExceptionThrown()

        when:
        configuration.xa = false

        then:
        ConfigurationException e = thrown()
        e.message == 'Datasource [default] cannot switch between an XA and a plain pool without a restart'

        cleanup:
        context?.close()
    }

    private static ApplicationContext xaContext(String database) {
        return ApplicationContext.run(
            'datasources.default.url': "jdbc:h2:mem:${database};DB_CLOSE_ON_EXIT=FALSE".toString(),
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.default.connection-factory-class-name': 'org.h2.jdbcx.JdbcDataSource',
            'datasources.default.xa': true
        )
    }

    private static int sessionId(Connection connection) {
        def resultSet = connection.createStatement().executeQuery("SELECT SESSION_ID()")
        resultSet.next()
        return resultSet.getInt(1)
    }
}
