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

class MySQLConfigurationSpec extends Specification{
    void "test vertx-mysql-client configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.mysql.client.port': '3306',
                'vertx.mysql.client.host': 'the-host',
                'vertx.mysql.client.database': 'the-db',
                'vertx.mysql.client.user': 'user',
                'vertx.mysql.client.password': 'secret',
                'vertx.mysql.client.maxSize': '5'
        )

        then:
        applicationContext.containsBean(MySQLConnectionConfiguration)
        applicationContext.getBean(MySQLConnectionConfiguration).connectOptions
        applicationContext.getBean(MySQLConnectionConfiguration).connectOptions.port == 3306
        applicationContext.getBean(MySQLConnectionConfiguration).connectOptions.host == 'the-host'
        applicationContext.getBean(MySQLConnectionConfiguration).connectOptions.database == 'the-db'
        applicationContext.getBean(MySQLConnectionConfiguration).connectOptions.user == 'user'
        applicationContext.getBean(MySQLConnectionConfiguration).connectOptions.password == 'secret'
        applicationContext.getBean(MySQLConnectionConfiguration).poolOptions.maxSize == 5


        cleanup:
        applicationContext?.stop()
    }
}
