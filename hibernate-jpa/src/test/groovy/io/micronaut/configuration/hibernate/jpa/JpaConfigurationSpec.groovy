/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.hibernate.jpa

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class JpaConfigurationSpec extends Specification {

    void "test copy of JPA configuration contains all properties"() {
        given:
        def ctx = ApplicationContext.run(
                'jpa.default.packages-to-scan':'my.package',
                'jpa.default.reactive':'true',
                'jpa.default.properties.hibernate.dialect':'org.hibernate.dialect.PostgreSQL95Dialect',
                'jpa.default.properties.hibernate.hbm2ddl.auto':'none',
                'jpa.default.mapping-resources':'hibernate/MyMapping.hbm.xml'
        )

        def config = ctx.getBean(JpaConfiguration)
        def configCopy = config.copy('configCopy')

        expect:
        config.entityScanConfiguration.enabled
        config.packagesToScan == ['my.package'] as String[]
        config.properties.get('hibernate.dialect') == 'org.hibernate.dialect.PostgreSQL95Dialect'
        config.properties.get('hibernate.hbm2ddl.auto') == 'none'
        config.mappingResources == ['hibernate/MyMapping.hbm.xml'] as List<String>
        config.reactive

        configCopy.entityScanConfiguration.enabled
        configCopy.packagesToScan == ['my.package'] as String[]
        configCopy.properties.get('hibernate.dialect') == 'org.hibernate.dialect.PostgreSQL95Dialect'
        configCopy.properties.get('hibernate.hbm2ddl.auto') == 'none'
        configCopy.mappingResources == ['hibernate/MyMapping.hbm.xml'] as List<String>

        config.reactive == configCopy.reactive

        cleanup:
        ctx?.close()
    }

    void "test copy operation of JPA configuration without all properties"() {
        given:
        def ctx = ApplicationContext.run(
                'jpa.default.packages-to-scan':'my.package'
        )

        def config = ctx.getBean(JpaConfiguration)
        def configCopy = config.copy('configCopy')

        expect:
        config.entityScanConfiguration.enabled
        config.packagesToScan == ['my.package'] as String[]
        config.properties.isEmpty()
        config.mappingResources.isEmpty()
        !config.reactive

        configCopy.entityScanConfiguration.enabled
        configCopy.packagesToScan == ['my.package'] as String[]
        configCopy.properties.isEmpty()
        configCopy.mappingResources.isEmpty()

        config.reactive == configCopy.reactive

        cleanup:
        ctx?.close()
    }

    void "test JPA entity scan configuration for packages-to-scan"() {
        given:
        def ctx = ApplicationContext.run(
                'jpa.default.packages-to-scan':'my.package'
        )

        def config = ctx.getBean(JpaConfiguration)

        expect:
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
        config.entityScanConfiguration.enabled
        config.packagesToScan == [] as String[]
        !config.entityScanConfiguration.findEntities().isEmpty()

        cleanup:
        ctx?.close()
    }

    void "test mapping resources"() {
        given:
        def ctx = ApplicationContext.run(
                'jpa.default.mapping-resources': ['hibernate/custom.hbm.xml']
        )

        def config = ctx.getBean(JpaConfiguration)

        expect:
        config.mappingResources
        config.mappingResources == ['hibernate/custom.hbm.xml']

        cleanup:
        ctx?.close()
    }

}
