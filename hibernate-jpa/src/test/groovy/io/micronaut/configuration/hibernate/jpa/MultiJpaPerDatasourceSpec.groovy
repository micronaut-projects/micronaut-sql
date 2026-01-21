package io.micronaut.configuration.hibernate.jpa

import io.micronaut.context.ApplicationContext
import org.hibernate.SessionFactory
import spock.lang.Specification

class MultiJpaPerDatasourceSpec extends Specification {

    void "two datasources with two jpa configs start and create two session factories"() {
        given:
        def ctx = ApplicationContext.run([
                'datasources.cat-database.url'             : 'jdbc:h2:mem:poc-cat;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.cat-database.driverClassName' : 'org.h2.Driver',
                'datasources.cat-database.username'        : 'sa',
                'datasources.cat-database.password'        : '',
                'datasources.cat-database.dialect'         : 'H2',

                'datasources.dog-database.url'             : 'jdbc:h2:mem:poc-dog;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.dog-database.driverClassName' : 'org.h2.Driver',
                'datasources.dog-database.username'        : 'sa',
                'datasources.dog-database.password'        : '',
                'datasources.dog-database.dialect'         : 'H2',

                'jpa.cat-database.properties.hibernate.hbm2ddl.auto': 'update',
                'jpa.cat-database.properties.hibernate.dialect'     : 'org.hibernate.dialect.H2Dialect',
                'jpa.cat-database.packages-to-scan'                 : 'io.micronaut.configuration.hibernate.jpa',

                'jpa.dog-database.properties.hibernate.hbm2ddl.auto': 'update',
                'jpa.dog-database.properties.hibernate.dialect'     : 'org.hibernate.dialect.H2Dialect',
                'jpa.dog-database.packages-to-scan'                 : 'io.micronaut.configuration.hibernate.jpa',
        ])

        when:
        def beans = ctx.getBeansOfType(SessionFactory)

        then:
        beans.size() == 2

        cleanup:
        ctx?.close()
    }

    void "two datasources with one jpa config fallback copy produces two session factories"() {
        given:
        def ctx = ApplicationContext.run([
                'datasources.cat-database.url'             : 'jdbc:h2:mem:poc-cat;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.cat-database.driverClassName' : 'org.h2.Driver',
                'datasources.cat-database.username'        : 'sa',
                'datasources.cat-database.password'        : '',
                'datasources.cat-database.dialect'         : 'H2',

                'datasources.dog-database.url'             : 'jdbc:h2:mem:poc-dog;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.dog-database.driverClassName' : 'org.h2.Driver',
                'datasources.dog-database.username'        : 'sa',
                'datasources.dog-database.password'        : '',
                'datasources.dog-database.dialect'         : 'H2',

                // Only one jpa config; entity scan covers both
                'jpa.cat-database.properties.hibernate.hbm2ddl.auto': 'update',
                'jpa.cat-database.properties.hibernate.dialect'     : 'org.hibernate.dialect.H2Dialect',
                'jpa.cat-database.packages-to-scan'                 : 'io.micronaut.configuration.hibernate.jpa'
        ])

        when:
        def beans = ctx.getBeansOfType(SessionFactory)

        then:
        beans.size() == 2

        cleanup:
        ctx?.close()
    }
}
