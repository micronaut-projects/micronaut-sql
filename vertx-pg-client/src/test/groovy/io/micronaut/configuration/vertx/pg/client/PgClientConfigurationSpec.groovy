/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.vertx.pg.client


import io.micronaut.context.ApplicationContext
import spock.lang.Specification


class PgClientConfigurationSpec extends Specification {

    void "test vertx-pg-client configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.pg.client.port': '5432',
                'vertx.pg.client.host': 'the-host',
                'vertx.pg.client.database': 'the-db',
                'vertx.pg.client.user': 'user',
                'vertx.pg.client.password': 'secret',
                'vertx.pg.client.maxSize': '5'
        )

        then:
        applicationContext.containsBean(PgClientConfiguration)
        applicationContext.getBean(PgClientConfiguration).connectOptions
        applicationContext.getBean(PgClientConfiguration).connectOptions.port == 5432
        applicationContext.getBean(PgClientConfiguration).connectOptions.host == 'the-host'
        applicationContext.getBean(PgClientConfiguration).connectOptions.database == 'the-db'
        applicationContext.getBean(PgClientConfiguration).connectOptions.user == 'user'
        applicationContext.getBean(PgClientConfiguration).connectOptions.password == 'secret'
        applicationContext.getBean(PgClientConfiguration).poolOptions.maxSize == 5


        cleanup:
        applicationContext?.stop()
    }



}
