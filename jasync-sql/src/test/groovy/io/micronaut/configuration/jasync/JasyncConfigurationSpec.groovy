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

package io.micronaut.configuration.jasync


import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class JasyncConfigurationSpec extends Specification {

    void "test jasync-client configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'jasync.client.port': '5432',
                'jasync.client.host': 'the-host',
                'jasync.client.database': 'the-db',
                'jasync.client.user': 'user',
                'jasync.client.password': 'secret',
                'jasync.client.maxSize': '5'
        )

        then:
        applicationContext.containsBean(JasyncPoolConfiguration)
        applicationContext.getBean(JasyncPoolConfiguration).jasyncOptions
        applicationContext.getBean(JasyncPoolConfiguration).jasyncOptions.port == 5432
        applicationContext.getBean(JasyncPoolConfiguration).jasyncOptions.host == 'the-host'
        applicationContext.getBean(JasyncPoolConfiguration).jasyncOptions.database == 'the-db'
        applicationContext.getBean(JasyncPoolConfiguration).jasyncOptions.user == 'user'
        applicationContext.getBean(JasyncPoolConfiguration).jasyncOptions.password == 'secret'
        applicationContext.getBean(JasyncPoolConfiguration).jasyncOptions.maxSize == 5


        cleanup:
        applicationContext?.stop()
    }



}
