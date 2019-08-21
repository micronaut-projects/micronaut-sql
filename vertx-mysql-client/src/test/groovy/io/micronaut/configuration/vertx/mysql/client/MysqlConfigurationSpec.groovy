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

package io.micronaut.configuration.vertx.mysql.client

import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class MysqlConfigurationSpec extends Specification{
    void "test reactive-pg-client configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.mysql.client.port': '3306',
                'vertx.mysql.client.host': 'the-host',
                'vertx.mysql.client.database': 'the-db',
                'vertx.mysql.client.user': 'user',
                'vertx.mysql.client.password': 'secret',
                'vertx.mysql.client.pool.maxSize': '5'
        )

        then:
        applicationContext.containsBean(MysqlConnectionConfiguration)
        applicationContext.getBean(MysqlConnectionConfiguration).connectOptions
        applicationContext.getBean(MysqlConnectionConfiguration).connectOptions.port == 3306
        applicationContext.getBean(MysqlConnectionConfiguration).connectOptions.host == 'the-host'
        applicationContext.getBean(MysqlConnectionConfiguration).connectOptions.database == 'the-db'
        applicationContext.getBean(MysqlConnectionConfiguration).connectOptions.user == 'user'
        applicationContext.getBean(MysqlConnectionConfiguration).connectOptions.password == 'secret'
        applicationContext.getBean(MysqlPoolConfiguration).poolOptions.maxSize == 5


        cleanup:
        applicationContext?.stop()
    }
}
