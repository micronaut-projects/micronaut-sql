package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import oracle.ucp.admin.UniversalConnectionPoolManager
import spock.lang.Specification
import spock.lang.Unroll

class UniversalConnectionPoolManagerFactorySpec extends Specification {

    void "the pool is not available when disabled is enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                [
                        "ucp-manager.enabled": false,
                ],
                "test")

        expect:
        !applicationContext.containsBean(UniversalConnectionPoolManager)
        !applicationContext.containsBean(ConnectionPoolManagerListener)

        cleanup:
        applicationContext.close()
    }

    @Unroll
    void "the mxbean is enabled: #enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                [
                        "ucp-manager.enabled"        : true,
                        "ucp-manager.jmx.enabled": enabled],
                "test")


        expect:
        applicationContext.getBean(UniversalConnectionPoolManager).isJmxEnabled() == enabled

        cleanup:
        applicationContext.close()

        where:
        enabled << [true, false]
    }
}
