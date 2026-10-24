package io.micronaut.configuration.vertx.oracle.client

import io.micronaut.context.ApplicationContext
import io.vertx.sqlclient.Pool
import spock.lang.Specification

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
