package io.micronaut.configuration.jdbc.hikari.custom

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.jdbc.DataSourceResolver
import spock.lang.Specification

import javax.sql.DataSource

class CustomDatasourceConfigurationSpec extends Specification {

    void "test default configuration and password change"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        System.setProperty("ds-default-password", "")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.password'             : '${ds-default-password}',
                 'datasources.default.dialect'              : 'H2',
                 'datasources.default.url'                  : 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                 'datasources.default.username'             : 'sa',
                 'datasources.default.driver-class-name'    : 'org.h2.Driver',
                 'datasources.default.connection-test-query': 'SELECT 1',
                 'use-custom-factory'                       : 'true']
        ))
        applicationContext.start()
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(DatasourceConfiguration)

        when:
        def dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource))

        then:
        dataSource instanceof CustomDataSource

        cleanup:
        applicationContext.close()
    }
}
