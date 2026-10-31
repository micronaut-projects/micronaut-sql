package io.micronaut.configuration.vertx.oracle.client

import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.ConfigurationException
import io.vertx.oracleclient.OracleConnectOptions
import io.vertx.sqlclient.Pool
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class OracleClientConfigurationSpec extends Specification {

    void "test vertx-oracle-client configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.port': '1522',
                'vertx.oracle.client.host': 'the-host',
                'vertx.oracle.client.service-name': 'FREEPDB1',
                'vertx.oracle.client.user': 'user',
                'vertx.oracle.client.password': 'secret',
                'vertx.oracle.client.fetch-size': '50',
                'vertx.oracle.client.max-size': '5'
        )
        OracleClientConfiguration configuration = applicationContext.getBean(OracleClientConfiguration)

        then:
        configuration.connectOptions.port == 1522
        configuration.connectOptions.host == 'the-host'
        configuration.connectOptions.serviceName == 'FREEPDB1'
        configuration.connectOptions.user == 'user'
        configuration.connectOptions.password == 'secret'
        configuration.connectOptions.fetchSize == 50
        configuration.poolOptions.maxSize == 5

        cleanup:
        applicationContext?.stop()
    }

    void "a TNS alias URI keeps the other configured options"() {
        given:
        OracleConnectOptions configured = new OracleConnectOptions()
                .setHost('the-host')
                .setPort(1522)
                .setServiceName('CONFIGURED')
                .setUser('user')
                .setPassword('secret')
                .setTnsAdmin('/wallet')
                .setFetchSize(50)

        when:
        OracleConnectOptions options = OracleClientFactory.fromUri(uri, configured)

        then:
        options.tnsAlias == 'mydb_high'
        options.host == OracleConnectOptions.DEFAULT_HOST
        options.port == OracleConnectOptions.DEFAULT_PORT
        options.serviceName == null
        options.tnsAdmin == '/wallet'
        options.fetchSize == 50
        options.user == 'user'
        options.password == 'secret'

        where:
        uri << ['oracle:thin:@mydb_high', 'jdbc:oracle:thin:@mydb_high', 'JDBC:oracle:thin:@mydb_high']
    }

    void "a URI replaces the configured endpoint and sets what it carries"() {
        given:
        OracleConnectOptions configured = new OracleConnectOptions()
                .setTnsAlias('configured_alias')
                .setUser('user')
                .setPassword('secret')
                .setFetchSize(50)

        when:
        OracleConnectOptions options = OracleClientFactory.fromUri('oracle:thin:scott/tiger@db-host:1523/FREEPDB1', configured)

        then:
        options.tnsAlias == null
        options.host == 'db-host'
        options.port == 1523
        options.serviceName == 'FREEPDB1'
        options.user == 'scott'
        options.password == 'tiger'
        options.fetchSize == 50
    }

    void "a URI with another scheme fails with a ConfigurationException"() {
        when:
        OracleClientFactory.fromUri('postgresql://localhost/db', new OracleConnectOptions())

        then:
        ConfigurationException e = thrown()
        e.message.startsWith('Invalid Oracle connection URI [postgresql://localhost/db]')
    }

    void "the Vert.x instance created for the pool is closed with the context"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.host': 'localhost',
                'vertx.oracle.client.service-name': 'FREEPDB1'
        )
        applicationContext.getBean(Pool)
        OracleClientFactory factory = applicationContext.getBean(OracleClientFactory)
        // Vert.x starts its threads on demand
        factory.createdVertx.executeBlocking { -> 'started' }.toCompletionStage().toCompletableFuture().get()

        expect:
        vertxThreads()

        when:
        applicationContext.close()

        then:
        factory.createdVertx == null
        new PollingConditions(timeout: 10).eventually {
            assert !vertxThreads()
        }
    }

    private static List<Thread> vertxThreads() {
        Thread.getAllStackTraces().keySet().findAll { it.alive && it.name.startsWith('vert.x-') }.toList()
    }

    void "test no pool is created without vertx-oracle-client configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run()

        then:
        !applicationContext.containsBean(OracleClientConfiguration)
        !applicationContext.containsBean(Pool)

        cleanup:
        applicationContext?.stop()
    }
}
