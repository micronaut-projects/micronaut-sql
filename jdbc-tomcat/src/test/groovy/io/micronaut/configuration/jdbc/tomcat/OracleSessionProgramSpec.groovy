package io.micronaut.configuration.jdbc.tomcat

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.jdbc.DataSourceResolver
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
        org.apache.tomcat.jdbc.pool.DataSource ds = resolver.resolve(ctx.getBean(DataSource))

        then:
        ds.getDbProperties().getProperty('v$session.program') == 'MyApp'

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
                'datasources.default.db-properties'         : ['v$session.program': 'UserSet']
        ]))
        ctx.start()
        DataSourceResolver resolver = ctx.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)

        when:
        org.apache.tomcat.jdbc.pool.DataSource ds = resolver.resolve(ctx.getBean(DataSource))

        then:
        ds.getDbProperties().getProperty('v$session.program') == 'UserSet'

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
        org.apache.tomcat.jdbc.pool.DataSource ds = resolver.resolve(ctx.getBean(DataSource))

        then:
        ds.getDbProperties().getProperty('v$session.program') == null

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
        org.apache.tomcat.jdbc.pool.DataSource ds = resolver.resolve(ctx.getBean(DataSource))

        then:
        ds.getDbProperties().getProperty('v$session.program') == null

        cleanup:
        ctx.close()
    }
}
