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
import org.jooq.Converter
import org.jooq.ConverterProvider
import org.jooq.JSON
import org.jooq.JSONB
import spock.lang.Specification

class JacksonConverterSpec extends Specification {

    void "test json conversion"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default'                               : [:],
                 'jooq.datasources.default.jackson-converter-enabled': true]
        ))
        applicationContext.start()

        ConverterProvider converterProvider = applicationContext.getBean(ConverterProvider)

        expect:
        converterProvider instanceof JacksonConverterProvider

        when:
        Converter jsonConverter = converterProvider.provide(JSON, TestObject)
        Converter jsonbConverter = converterProvider.provide(JSONB, TestObject)
        String serialized = "{\"name\":\"Test\",\"count\":3}"
        TestObject object = new TestObject("Test", 3)

        then:
        jsonConverter != null
        jsonbConverter != null

        jsonConverter.from(JSON.valueOf(serialized)) == object
        jsonbConverter.from(JSONB.valueOf(serialized)) == object

        jsonConverter.to(object) == JSON.valueOf(serialized)
        jsonbConverter.to(object) == JSONB.valueOf(serialized)

        cleanup:
        applicationContext.close()
    }

}
