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
