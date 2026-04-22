package io.micronaut.jdbc

import spock.lang.Specification

import javax.sql.DataSource
import java.sql.Connection
import java.sql.SQLException

class JdbcSqliteSupportSpec extends Specification {

    private static final String SQLITE_READ_ONLY_MESSAGE = "Cannot change read-only flag after establishing a connection. Use SQLiteConfig#setReadOnly and SQLiteConfig.createConnection()."
    private static final String SQLITE_READ_ONLY_MESSAGE_IN_OPEN_TRANSACTION = "Cannot change Read-Only status of this connection: the first statement was already executed and the transaction is open."

    void "sqlite connection wrapper tolerates unsupported read only toggles"() {
        given:
        Connection connection = Mock() {
            isReadOnly() >> false
            setReadOnly(true) >> { throw new SQLException(SQLITE_READ_ONLY_MESSAGE) }
            setReadOnly(false) >> { throw new SQLException(SQLITE_READ_ONLY_MESSAGE) }
        }

        when:
        Connection wrapped = JdbcSqliteSupport.wrapSqliteConnection(connection)
        wrapped.setReadOnly(true)

        then:
        noExceptionThrown()
        wrapped.isReadOnly()

        when:
        wrapped.setReadOnly(false)

        then:
        noExceptionThrown()
        !wrapped.isReadOnly()
    }

    void "sqlite connection wrapper rethrows unrelated failures"() {
        given:
        Connection connection = Mock() {
            isReadOnly() >> false
            setReadOnly(true) >> { throw new SQLException("boom") }
        }

        when:
        JdbcSqliteSupport.wrapSqliteConnection(connection).setReadOnly(true)

        then:
        SQLException e = thrown()
        e.message == "boom"
    }

    void "sqlite connection wrapper tolerates open transaction read only failure"() {
        given:
        Connection connection = Mock() {
            isReadOnly() >> false
            setReadOnly(true) >> { throw new SQLException(SQLITE_READ_ONLY_MESSAGE_IN_OPEN_TRANSACTION) }
        }

        when:
        Connection wrapped = JdbcSqliteSupport.wrapSqliteConnection(connection)
        wrapped.setReadOnly(true)

        then:
        noExceptionThrown()
        wrapped.isReadOnly()
    }

    void "sqlite datasource wrapper wraps connections and resolver unwraps target datasource"() {
        given:
        Connection connection = Mock() {
            isReadOnly() >> false
            setReadOnly(true) >> { throw new SQLException(SQLITE_READ_ONLY_MESSAGE) }
        }
        DataSource dataSource = Mock() {
            getConnection() >> connection
        }

        when:
        DataSource wrapped = JdbcSqliteSupport.wrapDataSource(dataSource, "org.sqlite.JDBC", null)
        Connection wrappedConnection = wrapped.getConnection()

        then:
        wrapped !== dataSource
        new SQLiteAwareDataSourceResolver().resolve(wrapped).is(dataSource)

        when:
        wrappedConnection.setReadOnly(true)

        then:
        noExceptionThrown()
        wrappedConnection.isReadOnly()
    }

    void "non sqlite datasource is returned unchanged"() {
        given:
        DataSource dataSource = Mock()

        expect:
        JdbcSqliteSupport.wrapDataSource(dataSource, "org.h2.Driver", "jdbc:h2:mem:test").is(dataSource)
    }
}
