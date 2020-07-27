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
package io.micronaut.configuration.jasync

import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.interceptor.LoggingInterceptorSupplier
import com.github.jasync.sql.db.interceptor.MdcQueryInterceptorSupplier
import com.github.jasync.sql.db.interceptor.QueryInterceptor
import io.micronaut.context.ApplicationContext
import spock.lang.Specification

class JasyncConfigurationSpec extends Specification {

    void "test jasync-client configuration"() {
        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'jasync.client.port': '5433',
                'jasync.client.host': 'the-host',
                'jasync.client.database': 'the-db',
                'jasync.client.username': 'user',
                'jasync.client.password': 'secret',
                'jasync.client.maxActiveConnections': '5',
                'jasync.client.ssl.mode': 'Prefer',
                'jasync.client.ssl.rootCert': 'some.cert'
        )

        then:
        applicationContext.containsBean(JasyncPoolConfiguration)
        def config = applicationContext.getBean(JasyncPoolConfiguration)
        config.jasyncOptions
        config.jasyncOptions.port == 5433
        config.jasyncOptions.database == 'the-db'
        config.jasyncOptions.username == 'user'
        config.jasyncOptions.password == 'secret'
        config.jasyncOptions.maxActiveConnections == 5
        config.jasyncOptions.host == 'the-host'
        config.jasyncOptions.ssl.mode == SSLConfiguration.Mode.Prefer
        config.jasyncOptions.ssl.rootCert == new File("some.cert")

        cleanup:
        applicationContext?.stop()
    }

    void "test jasync-client interceptors configuration"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'jasync.client.port': '5433'
        )

        when:
        applicationContext.registerSingleton(new MdcQueryInterceptorSupplier().get())
        applicationContext.registerSingleton(new LoggingInterceptorSupplier().get())

        then:
        applicationContext.containsBean(JasyncPoolConfiguration)
        applicationContext.containsBean(QueryInterceptor)

        def config = applicationContext.getBean(JasyncPoolConfiguration)
        config.jasyncOptions
        config.jasyncOptions.interceptors
        config.jasyncOptions.interceptors.size() == 2
    }
}
