package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import spock.lang.Specification


class UniversalConnectionPoolManagerConfigurationSpec extends Specification {

    void "default is the ucp pool is enabled and jmx disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("test")

        expect:
        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration)
        applicationContext.getBean(UniversalConnectionPoolManagerConfiguration).isEnabled()

        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration.JMXBeanConfiguration)
        !applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.JMXBeanConfiguration).isEnabled()

        cleanup:
        applicationContext.close()
    }

    void "test non defaults"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                [
                        "ucp-manager.enabled"    : false,
                        "ucp-manager.jmx.enabled": true],
                "test")

        expect:
        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration)
        !applicationContext.getBean(UniversalConnectionPoolManagerConfiguration).isEnabled()

        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration.JMXBeanConfiguration)
        applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.JMXBeanConfiguration).isEnabled()

        cleanup:
        applicationContext.close()
    }
}