package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.UCPConnectionBuilder
import spock.lang.Specification

import javax.sql.XAConnection
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.ShardingKey
import java.time.Duration

class XALocalConnectionSpec extends Specification {

    private static final Set<String> OWN_METHODS = ['close', 'isClosed', 'unwrap', 'isWrapperFor'] as Set

    void "every Connection method is delegated to the local connection of the XA connection"() {
        given:
        List<String> calls = []
        Connection delegate = (Connection) Proxy.newProxyInstance(getClass().classLoader, [Connection] as Class[], { proxy, Method method, Object[] args ->
            calls << method.name
            return defaultValue(method.returnType)
        } as InvocationHandler)
        XAConnection xaConnection = Mock()
        XALocalConnection connection = new XALocalConnection(xaConnection, delegate)
        List<Method> delegated = Connection.methods.findAll { !Modifier.isStatic(it.modifiers) && !OWN_METHODS.contains(it.name) }

        when:
        delegated.each { Method method ->
            calls.clear()
            method.invoke(connection, method.parameterTypes.collect { defaultValue(it) } as Object[])
            assert calls == [method.name]
        }

        then:
        delegated.size() > 40
        0 * xaConnection.close()
    }

    void "the connection builder of an XA pool passes every setting to the XA connection builder"() {
        given:
        ApplicationContext context = ApplicationContext.run(
            'datasources.default.url': 'jdbc:h2:mem:xaBuilderSettings;DB_CLOSE_ON_EXIT=FALSE',
            'datasources.default.username': 'sa',
            'datasources.default.password': '',
            'datasources.default.connection-factory-class-name': 'org.h2.jdbcx.JdbcDataSource',
            'datasources.default.xa': true
        )
        PoolDataSource pool = context.getBean(DatasourceConfiguration).poolDataSource

        when:
        UCPConnectionBuilder builder = pool.createConnectionBuilder()

        then: "each setting returns the builder, so settings can be chained"
        builder.user('sa').is(builder)
        builder.password('').is(builder)
        builder.labels(new Properties()).is(builder)
        builder.serviceName('orders').is(builder)
        builder.proxyProperties(0, new Properties()).is(builder)
        builder.shardingKey((oracle.jdbc.OracleShardingKey) null).is(builder)
        builder.superShardingKey((oracle.jdbc.OracleShardingKey) null).is(builder)
        builder.shardingKey((ShardingKey) null).is(builder)
        builder.superShardingKey((ShardingKey) null).is(builder)
        builder.pdbRoles(new Properties()).is(builder)
        builder.connectionWaitTimeout(5).is(builder)
        builder.connectionWaitDuration(Duration.ofSeconds(5)).is(builder)

        cleanup:
        context?.close()
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null
        }
        switch (type) {
            case boolean: return false
            case long: return 0L
            case void: return null
            default: return 0
        }
    }
}
