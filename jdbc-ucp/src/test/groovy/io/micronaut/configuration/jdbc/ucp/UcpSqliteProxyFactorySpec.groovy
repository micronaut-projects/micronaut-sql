package io.micronaut.configuration.jdbc.ucp

import io.micronaut.jdbc.DataSourceResolver
import io.micronaut.jdbc.SQLiteAwareDataSourceResolver
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Specification

import java.sql.Connection
import java.sql.SQLException

class UcpSqliteProxyFactorySpec extends Specification {

    private static final String SQLITE_READ_ONLY_MESSAGE = "Cannot change read-only flag after establishing a connection. Use SQLiteConfig#setReadOnly and SQLiteConfig.createConnection()."

    void "sqlite proxy preserves pool data source type and unwraps target"() {
        given:
        Connection connection = Mock() {
            isReadOnly() >> false
            setReadOnly(true) >> { throw new SQLException(SQLITE_READ_ONLY_MESSAGE) }
        }
        PoolDataSource target = Mock() {
            getConnection() >> connection
        }
        DataSourceResolver resolver = new SQLiteAwareDataSourceResolver()

        when:
        PoolDataSource proxied = UcpSqliteProxyFactory.wrap(target, "org.sqlite.JDBC", "jdbc:sqlite:file:test")

        then:
        proxied instanceof PoolDataSource
        resolver.resolve(proxied).is(target)

        when:
        Connection wrapped = proxied.getConnection()
        wrapped.setReadOnly(true)

        then:
        noExceptionThrown()
        wrapped.isReadOnly()
    }

    void "non sqlite pool data source is unchanged"() {
        given:
        PoolDataSource target = Mock()

        expect:
        UcpSqliteProxyFactory.wrap(target, "org.h2.Driver", "jdbc:h2:mem:test").is(target)
    }
}
