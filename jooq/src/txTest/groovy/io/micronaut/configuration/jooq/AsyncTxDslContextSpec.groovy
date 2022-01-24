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
import org.jooq.DSLContext
import spock.lang.Specification

class AsyncTxDslContextSpec extends Specification {

    void "test simple async sql"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['r2dbc.datasources.default.dialect'         : 'h2',
                 'r2dbc.datasources.default.username'        : 'sa',
                 'r2dbc.datasources.default.password'        : '',
                 'r2dbc.datasources.default.url'             : 'r2dbc:h2:mem:///testdb',
                 "r2dbc.datasources.default.options.DB_CLOSE_DELAY": "10",
                 "r2dbc.datasources.default.options.protocol"      : "mem",
                 'jooq.r2dbc-datasources.default.sql-dialect': 'h2']
        ))
        applicationContext.start()
        DSLContext db = applicationContext.getBean(DSLContext)
        AsyncTxTestService service = applicationContext.getBean(AsyncTxTestService)

        when:
        service.abortTransaction().blockLast()
        service.count().blockLast()

        then:
        RuntimeException ex = thrown()
        ex.message == "count=2"

        cleanup:
        applicationContext.close()
    }


}
