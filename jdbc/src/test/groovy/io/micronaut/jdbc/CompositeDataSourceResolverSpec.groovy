package io.micronaut.jdbc

import io.micronaut.context.ApplicationContext
import jakarta.inject.Singleton
import spock.lang.Specification

import javax.sql.DataSource

class CompositeDataSourceResolverSpec extends Specification {

    void "primary bean of type DataSourceResolver is CompositeDataSourceResolver"() {
        given:
        ApplicationContext context = ApplicationContext.run()

        when:
        def dataSourceResolver = context.getBean(DataSourceResolver)
        // Would throw NonUniqueBeanException if not marked @Primary:
        // Multiple possible bean candidates found: [CompositeDataSourceResolver, StubDataSourceResolver]

        then:
        dataSourceResolver instanceof CompositeDataSourceResolver

        cleanup:
        context.close()
    }

    @Singleton
    static class StubDataSourceResolver implements DataSourceResolver {

        @Override
        DataSource resolve(DataSource dataSource) {
            return dataSource
        }
    }
}
