package io.micronaut.configuration.jasync

import com.github.jasync.sql.db.interceptor.LoggingInterceptorSupplier
import com.github.jasync.sql.db.interceptor.MdcQueryInterceptorSupplier
import com.github.jasync.sql.db.interceptor.QueryInterceptor

class JasyncQueryInterceptorsSpec extends ApplicationContextSpecification {

    @Override
    Map<String, Object> getConfiguration() {
        super.configuration + ['jasync.client.port': '5433']
    }

    void "test jasync-client interceptors configuration"() {
        given:
        List<QueryInterceptor> queryInterceptors = [
                new MdcQueryInterceptorSupplier().get(),
                new LoggingInterceptorSupplier().get()
        ]

        when:
        queryInterceptors.each { queryInterceptor ->
            applicationContext.registerSingleton(queryInterceptor)
        }

        then:
        applicationContext.containsBean(JasyncPoolConfiguration)
        applicationContext.containsBean(QueryInterceptor)
        applicationContext.getBeansOfType(QueryInterceptor).size() == queryInterceptors.size()

        and:
        def config = applicationContext.getBean(JasyncPoolConfiguration)
        config.jasyncOptions
        config.jasyncOptions.interceptors
        config.jasyncOptions.interceptors.size() == queryInterceptors.size()
    }
}
