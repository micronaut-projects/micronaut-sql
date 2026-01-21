package io.micronaut.configuration.jdbc.dbcp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.jdbc.DataSourceResolver
import org.apache.commons.dbcp2.BasicDataSource
import spock.lang.Specification

import javax.sql.DataSource

class OracleSessionProgramSpec extends Specification {

    void "sets program for oracle dialect by default"() {
        given:
        ApplicationContext ctx = new DefaultApplicationContext("test")
        ctx.environment.addPropertySource(MapPropertySource.of('test', [
                'micronaut.application.name'           : 'MyApp',
                'datasources.default'                  : [:],
                'datasources.default.dialect'          : 'oracle',
                'datasources.default.url'              : 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'         : 'sa',
                'datasources.default.password'         : ''
        ]))
        ctx.start()
        DataSourceResolver resolver = ctx.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        DatasourceConfiguration conf = resolver.resolve(ctx.getBean(DatasourceConfiguration))

        then:
        conf.oracleProgramProvided

        cleanup:
        ctx.close()
    }

    void "respects user override for program"() {
        given:
        ApplicationContext ctx = new DefaultApplicationContext("test")
        ctx.environment.addPropertySource(MapPropertySource.of('test', [
                'datasources.default'                       : [:],
                'datasources.default.dialect'               : 'oracle',
                'datasources.default.url'                   : 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'              : 'sa',
                'datasources.default.password'              : '',
                'datasources.default.data-source-properties': ['v$session.program': 'UserSet']
        ]))
        ctx.start()
        DataSourceResolver resolver = ctx.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        DatasourceConfiguration conf = resolver.resolve(ctx.getBean(DatasourceConfiguration))

        then:
        conf.oracleProgramProvided

        cleanup:
        ctx.close()
    }

    void "disable flag prevents setting program"() {
        given:
        ApplicationContext ctx = new DefaultApplicationContext("test")
        ctx.environment.addPropertySource(MapPropertySource.of('test', [
                'micronaut.application.name'           : 'MyApp',
                'datasources.default'                  : [:],
                'datasources.default.dialect'          : 'oracle',
                'datasources.default.oracle.session.enabled': false,
                'datasources.default.url'              : 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'         : 'sa',
                'datasources.default.password'         : ''
        ]))
        ctx.start()
        DataSourceResolver resolver = ctx.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        DatasourceConfiguration conf = resolver.resolve(ctx.getBean(DatasourceConfiguration))

        then:
        !conf.oracleProgramProvided

        cleanup:
        ctx.close()
    }

    void "non-oracle does not set program"() {
        given:
        ApplicationContext ctx = new DefaultApplicationContext("test")
        ctx.environment.addPropertySource(MapPropertySource.of('test', [
                'micronaut.application.name'           : 'MyApp',
                'datasources.default'                  : [:],
                'datasources.default.url'              : 'jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username'         : 'sa',
                'datasources.default.password'         : ''
        ]))
        ctx.start()
        DataSourceResolver resolver = ctx.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        DatasourceConfiguration conf = resolver.resolve(ctx.getBean(DatasourceConfiguration))

        then:
        !conf.oracleProgramProvided

        cleanup:
        ctx.close()
    }
}
