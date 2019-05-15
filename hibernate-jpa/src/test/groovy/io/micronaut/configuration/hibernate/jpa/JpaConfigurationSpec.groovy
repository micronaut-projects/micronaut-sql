package io.micronaut.configuration.hibernate.jpa

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class JpaConfigurationSpec extends Specification {

    void "test JPA entity scan configuration for packages-to-scan"() {
        given:
        def ctx = ApplicationContext.run(
                'jpa.default.packages-to-scan':'my.package'
        )

        def config = ctx.getBean(JpaConfiguration)

        expect:
        config.entityScanConfiguration.classpath
        config.entityScanConfiguration.enabled
        config.packagesToScan == ['my.package'] as String[]


        cleanup:
        ctx?.close()
    }

    void "test JPA entity default packages to scan config"() {
        given:
        def ctx = ApplicationContext.run(
                'jpa.default.properties.foo':'bar'
        )

        def config = ctx.getBean(JpaConfiguration)

        expect:
        !config.entityScanConfiguration.classpath
        config.entityScanConfiguration.enabled
        config.packagesToScan == [] as String[]
        !config.entityScanConfiguration.findEntities().isEmpty()

        cleanup:
        ctx?.close()
    }
}
