/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.configuration.hibernate.reactive

import io.micronaut.context.ApplicationContext
import org.hibernate.SessionFactory
import spock.lang.Specification

class DisabledReactiveJpaConfigurationSpec extends Specification {

    void "a disabled reactive JPA configuration starts without its database"() {
        given: "a reactive configuration whose database port has nothing listening"
            Map<String, Object> config = [
                    'jpa.default.reactive'                                : true,
                    'jpa.default.enabled'                                 : false,
                    'jpa.default.entity-scan.packages'                    : ['io.micronaut.configuration.hibernate.reactive.datasources.db1'],
                    'jpa.default.properties.hibernate.connection.url'     : 'jdbc:postgresql://localhost:1/unreachable',
                    'jpa.default.properties.hibernate.connection.username': 'test',
                    'jpa.default.properties.hibernate.connection.password': 'test',
            ]

        when:
            def context = ApplicationContext.run(config)

        then:
            context.getBeansOfType(SessionFactory).isEmpty()

        cleanup:
            context?.close()
    }
}
