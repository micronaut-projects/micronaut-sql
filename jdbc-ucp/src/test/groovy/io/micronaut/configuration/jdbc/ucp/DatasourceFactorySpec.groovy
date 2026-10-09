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
package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.BeanLocator
import io.micronaut.context.annotation.Retain
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.core.value.PropertyResolver
import io.micronaut.inject.BeanDefinition
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceFactorySpec extends Specification {

    def "the factory and its pools receive nothing bound to the context, so that development mode can retain them"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default.url'     : 'jdbc:h2:mem:retainable;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'datasources.default.username': 'sa',
                'datasources.default.password': ''
        ])

        when:
        Collection<BeanDefinition<?>> pools = applicationContext.getBeanDefinitions(DataSource)
        Collection<BeanDefinition<?>> closure = [applicationContext.getBeanDefinition(DatasourceFactory)] + pools +
                applicationContext.getBeanDefinitions(DatasourceConfiguration)

        then:
        // the factory receives no connection pool manager, whose bean receives a configuration implemented by introduction advice
        !pools.isEmpty()
        pools.every { it.stringValues(Retain, "invalidatedBy") == ["datasources", "ucp-manager"] as String[] }
        closure.every { BeanDefinition<?> definition ->
            !definition.proxy && definition.requiredComponents.every { Class<?> type ->
                !BeanLocator.isAssignableFrom(type)
                        && !PropertyResolver.isAssignableFrom(type)
                        && !ApplicationEventPublisher.isAssignableFrom(type)
            }
        }

        cleanup:
        applicationContext.close()
    }
}
