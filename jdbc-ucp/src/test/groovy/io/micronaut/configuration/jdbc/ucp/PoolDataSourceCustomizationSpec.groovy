package io.micronaut.configuration.jdbc.ucp

import io.micronaut.configuration.jdbc.ucp.test.ConnectionCreationRecorder
import io.micronaut.context.ApplicationContext
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Specification

import javax.sql.DataSource

class PoolDataSourceCustomizationSpec extends Specification {

    void "a customizer configures the pool before it starts"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'spec.name'                           : 'PoolDataSourceCustomizationSpec',
                'datasources.default.url'             : 'jdbc:h2:mem:customization;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'        : 'sa',
                'datasources.default.password'        : '',
                'datasources.default.initial-pool-size': 2
        ], 'test')

        when:
        PoolDataSource dataSource = applicationContext.getBean(DataSource).unwrap(PoolDataSource)
        ConnectionCreationRecorder recorder = applicationContext.getBean(ConnectionCreationRecorder)

        then:
        dataSource.getConnectionCreationConsumer() != null
        recorder.customizedDataSources == ["default"]
        recorder.createdConnections.size() >= 2

        cleanup:
        applicationContext.close()
    }

    void "UCP lifecycle and failover settings are configurable"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'                            : 'jdbc:h2:mem:lifecycle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'                       : 'sa',
                'datasources.default.password'                       : '',
                'datasources.default.max-connection-reuse-time'      : 300,
                'datasources.default.max-connection-reuse-count'     : 1000,
                'datasources.default.connection-harvest-trigger-count': 5,
                'datasources.default.connection-harvest-max-count'   : 2,
                'datasources.default.fast-connection-failover-enabled': false
        ], 'test')

        when:
        PoolDataSource dataSource = applicationContext.getBean(DataSource).unwrap(PoolDataSource)

        then:
        dataSource.getMaxConnectionReuseTime() == 300
        dataSource.getMaxConnectionReuseCount() == 1000
        dataSource.getConnectionHarvestTriggerCount() == 5
        dataSource.getConnectionHarvestMaxCount() == 2
        !dataSource.getFastConnectionFailoverEnabled()

        cleanup:
        applicationContext.close()
    }
}
