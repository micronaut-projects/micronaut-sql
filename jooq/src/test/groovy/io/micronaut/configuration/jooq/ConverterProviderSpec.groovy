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
package io.micronaut.configuration.jooq


import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.inject.qualifiers.Qualifiers
import org.jooq.ConverterProvider
import org.jooq.DSLContext
import org.jooq.impl.DefaultConverterProvider
import spock.lang.Specification

class ConverterProviderSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.getBean(DSLContext).configuration().converterProvider() instanceof DefaultConverterProvider

        cleanup:
        applicationContext.close()
    }

    void "test custom converter provider"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.registerSingleton(ConverterProvider, new CustomConverterProvider(), Qualifiers.byName("default"))
        applicationContext.start()

        expect:
        applicationContext.getBean(DSLContext).configuration().converterProvider() instanceof CustomConverterProvider

        cleanup:
        applicationContext.close()
    }

    void "test built-in jackson converter provider"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default'     : [:],
                 'jooq.datasources.default.jackson-converter-enabled': true]
        ))
        applicationContext.start()

        expect:
        applicationContext.getBean(DSLContext).configuration().converterProvider() instanceof JacksonConverterProvider

        cleanup:
        applicationContext.close()
    }

}
