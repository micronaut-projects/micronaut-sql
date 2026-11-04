package io.micronaut.configuration.jdbc.ucp

import io.micronaut.configuration.jdbc.ucp.test.ConnectionCreationRecorder
import io.micronaut.context.ApplicationContext
import io.micronaut.context.exceptions.ConfigurationException
import oracle.ucp.ConnectionCreationInformation
import oracle.ucp.jdbc.PoolDataSource
import oracle.ucp.jdbc.PoolDataSourceImpl
import spock.lang.Specification

import javax.sql.DataSource
import java.sql.SQLException

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
        recorder.createdConnections*.status == [ConnectionCreationInformation.Status.SUCCESS] * 2

        cleanup:
        applicationContext.close()
    }

    void "a customizer failure names the data source"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'     : 'jdbc:h2:mem:failure;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username': 'sa',
                'datasources.default.password': ''
        ], 'test')
        DatasourceFactory factory = applicationContext.getBean(DatasourceFactory)
        DatasourceConfiguration configuration = applicationContext.getBean(DatasourceConfiguration)
        PoolDataSourceCustomizer failing = { String name, PoolDataSource pool -> throw new SQLException("bad setting") }

        when:
        factory.dataSource(configuration, [failing])

        then:
        ConfigurationException e = thrown()
        e.message == 'Failed to customize data source [default]: bad setting'
        e.cause instanceof SQLException

        cleanup:
        applicationContext.close()
    }

    @SuppressWarnings('GrDeprecatedAPIUsage')
    void "the deprecated factory method applies no customizers"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'     : 'jdbc:h2:mem:deprecated;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username': 'sa',
                'datasources.default.password': ''
        ], 'test')
        DatasourceFactory factory = applicationContext.getBean(DatasourceFactory)
        DatasourceConfiguration configuration = applicationContext.getBean(DatasourceConfiguration)

        expect:
        factory.dataSource(configuration).is(configuration.getPoolDataSource())

        cleanup:
        applicationContext.close()
    }

    void "UCP connection reuse and harvesting settings are configurable"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'                            : 'jdbc:h2:mem:lifecycle;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'                       : 'sa',
                'datasources.default.password'                       : '',
                'datasources.default.max-connection-reuse-time'      : 300,
                'datasources.default.max-connection-reuse-count'     : 1000,
                'datasources.default.connection-harvest-trigger-count': 5,
                'datasources.default.connection-harvest-max-count'   : 2
        ], 'test')

        when:
        PoolDataSource dataSource = applicationContext.getBean(DataSource).unwrap(PoolDataSource)

        then:
        dataSource.getMaxConnectionReuseTime() == 300
        dataSource.getMaxConnectionReuseCount() == 1000
        dataSource.getConnectionHarvestTriggerCount() == 5
        dataSource.getConnectionHarvestMaxCount() == 2

        cleanup:
        applicationContext.close()
    }

    void "Fast Connection Failover is off by default"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'     : 'jdbc:h2:mem:fandefault;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username': 'sa',
                'datasources.default.password': ''
        ], 'test')

        expect:
        !applicationContext.getBean(DataSource).unwrap(PoolDataSource).getFastConnectionFailoverEnabled()

        cleanup:
        applicationContext?.close()
    }

    void "Fast Connection Failover and the ONS configuration bind with the key #onsKey"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'                             : 'jdbc:h2:mem:fan;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'                        : 'sa',
                'datasources.default.password'                        : '',
                'datasources.default.fast-connection-failover-enabled': true,
                ('datasources.default.' + onsKey)                     : 'nodes=racnode1:6200,racnode2:6200'
        ], 'test')
        PoolDataSource dataSource = applicationContext.getBean(DataSource).unwrap(PoolDataSource)

        expect:
        dataSource.getFastConnectionFailoverEnabled()
        dataSource.getONSConfiguration() == 'nodes=racnode1:6200,racnode2:6200'

        cleanup:
        applicationContext?.close()

        where:
        onsKey << ['ons-configuration', 'onsConfiguration', 'onsconfiguration', 'ONSConfiguration']
    }

    void "an ONS configuration the pool rejects fails with a ConfigurationException"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run('test')
        DatasourceConfiguration configuration = new DatasourceConfiguration('default', applicationContext.environment)
        configuration.delegate = new PoolDataSourceImpl() {
            @Override
            void setONSConfiguration(String onsConfiguration) throws SQLException {
                throw new SQLException('invalid ONS configuration')
            }
        }

        when:
        configuration.setOnsConfiguration('nodes=')

        then:
        ConfigurationException e = thrown()
        e.message == 'Unable to set datasource ONS configuration: invalid ONS configuration'
        e.cause instanceof SQLException

        cleanup:
        applicationContext?.close()
    }
}
