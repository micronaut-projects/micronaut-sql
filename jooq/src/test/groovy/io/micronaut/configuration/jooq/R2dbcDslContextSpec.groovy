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
import io.r2dbc.spi.ConnectionFactory
import io.r2dbc.spi.ConnectionFactoryMetadata
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import spock.lang.Specification

import javax.sql.DataSource

class R2dbcDslContextSpec extends Specification {

    void "test r2dbc configuration creates dsl context without jdbc datasource"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.registerSingleton(ConnectionFactory, Stub(ConnectionFactory) {
            getMetadata() >> Stub(ConnectionFactoryMetadata) {
                getName() >> "stub"
            }
        }, Qualifiers.byName("default"))
        applicationContext.start()

        expect:
        !applicationContext.containsBean(DataSource)
        applicationContext.containsBean(ConnectionFactory)
        applicationContext.containsBean(Configuration)
        applicationContext.containsBean(DSLContext)
        applicationContext.getBean(DSLContext).configuration().dialect() == SQLDialect.DEFAULT

        cleanup:
        applicationContext.close()
    }

    void "test r2dbc sql dialect override"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
            "test",
            ["jooq.r2dbc-datasources.default.sql-dialect": "POSTGRES"]
        ))
        applicationContext.registerSingleton(ConnectionFactory, Stub(ConnectionFactory) {
            getMetadata() >> Stub(ConnectionFactoryMetadata) {
                getName() >> "stub"
            }
        }, Qualifiers.byName("default"))
        applicationContext.start()

        when:
        Configuration configuration = applicationContext.getBean(DSLContext).configuration()

        then:
        configuration.dialect() == SQLDialect.POSTGRES

        cleanup:
        applicationContext.close()
    }
}
