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
package io.micronaut.configuration.jdbc.hikari

import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.ApplicationEventPublisher
import io.micronaut.context.BeanLocator
import io.micronaut.core.value.PropertyResolver
import io.micronaut.inject.BeanDefinition
import spock.lang.Specification

class DatasourceFactorySpec extends Specification {

    def "wire class with constructor"() {
        expect:
        new DatasourceFactory()
    }

    def "the factory and its pools receive nothing bound to the context, so that development mode can retain them"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(['datasources.default': [:]])

        when:
        BeanDefinition<DatasourceFactory> factory = applicationContext.getBeanDefinition(DatasourceFactory)
        Collection<BeanDefinition<?>> pools = applicationContext.getBeanDefinitions(javax.sql.DataSource)

        then:
        factory.constructor.arguments.length == 0
        !pools.isEmpty()
        pools.every { it.stringValues(io.micronaut.context.annotation.Retain, "invalidatedBy") == ["datasources"] as String[] }
        ([factory] + pools + applicationContext.getBeanDefinitions(DatasourceConfiguration)).every { BeanDefinition<?> definition ->
            definition.requiredComponents.every { Class<?> type ->
                !BeanLocator.isAssignableFrom(type)
                        && !PropertyResolver.isAssignableFrom(type)
                        && !ApplicationEventPublisher.isAssignableFrom(type)
            }
        }

        cleanup:
        applicationContext.close()
    }
}
