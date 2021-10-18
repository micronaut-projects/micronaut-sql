package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import spock.lang.Specification


class UniversalConnectionPoolManagerConfigurationSpec extends Specification {

    void "all is enabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("test")

        expect:
        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration)
        applicationContext.getBean(UniversalConnectionPoolManagerConfiguration).isEnabled()

        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration.MxBeanConfiguration)
        applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.MxBeanConfiguration).isEnabled()

        cleanup:
        applicationContext.close()
    }

    void "all is disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                [
                        "ucp-manager.enabled"        : false,
                        "ucp-manager.mx-bean.enabled": false],
                "test")

        expect:
        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration)
        !applicationContext.getBean(UniversalConnectionPoolManagerConfiguration).isEnabled()

        applicationContext.containsBean(UniversalConnectionPoolManagerConfiguration.MxBeanConfiguration)
        !applicationContext.getBean(UniversalConnectionPoolManagerConfiguration.MxBeanConfiguration).isEnabled()

        cleanup:
        applicationContext.close()
    }
}