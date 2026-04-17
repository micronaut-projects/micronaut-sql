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
package io.micronaut.configuration.vertx.pg.client


import io.micronaut.context.ApplicationContext
import io.vertx.core.net.PemTrustOptions
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.SslMode
import io.vertx.sqlclient.Pool
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

    void "test rxjava3 sql client pool bean is exposed when rxjava3 is on the classpath"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.pg.client.port': '5432',
                'vertx.pg.client.host': 'the-host',
                'vertx.pg.client.database': 'the-db',
                'vertx.pg.client.user': 'user',
                'vertx.pg.client.password': 'secret',
                'vertx.pg.client.maxSize': '5'
        )

        when:
        Pool pool = applicationContext.getBean(Pool)

        then:
        applicationContext.containsBean(io.vertx.rxjava3.sqlclient.Pool)
        applicationContext.getBean(io.vertx.rxjava3.sqlclient.Pool).delegate.is(pool)

        cleanup:
        applicationContext?.stop()
    }

    void "test vertx-pg-client connects with direct options when verify-ca trust options are configured"() {
        given:
        int port = findFreePort()

        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.pg.client.host': 'localhost',
                'vertx.pg.client.port': port,
                'vertx.pg.client.ssl': true,
                'vertx.pg.client.ssl-mode': 'VERIFY_CA',
                'vertx.pg.client.pem-trust-options.cert-paths[0]': 'certs/ca.crt'
        )
        PgConnectOptions options = PgConnectOptionsResolver.resolve(
                applicationContext.getBean(PgClientConfiguration),
                applicationContext.getBean(PgPemTrustOptionsConfiguration)
        )
        PemTrustOptions trustOptions = (PemTrustOptions) options.sslOptions.trustOptions

        then:
        options.host == 'localhost'
        options.port == port
        options.sslMode == SslMode.VERIFY_CA
        options.sslOptions != null
        trustOptions.certPaths == ['certs/ca.crt']

        cleanup:
        applicationContext?.stop()
    }

    void "test vertx-pg-client uri mode keeps verify-ca trust options during connect"() {
        given:
        int port = findFreePort()

        when:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.pg.client.uri': "postgresql://user:secret@localhost:${port}/the-db",
                'vertx.pg.client.ssl': true,
                'vertx.pg.client.ssl-mode': 'VERIFY_CA',
                'vertx.pg.client.pem-trust-options.cert-paths[0]': 'certs/ca.crt'
        )
        PgConnectOptions options = PgConnectOptionsResolver.resolve(
                applicationContext.getBean(PgClientConfiguration),
                applicationContext.getBean(PgPemTrustOptionsConfiguration)
        )
        PemTrustOptions trustOptions = (PemTrustOptions) options.sslOptions.trustOptions

        then:
        options.host == 'localhost'
        options.port == port
        options.database == 'the-db'
        options.user == 'user'
        options.password == 'secret'
        options.sslMode == SslMode.VERIFY_CA
        options.sslOptions != null
        trustOptions.certPaths == ['certs/ca.crt']

        cleanup:
        applicationContext?.stop()
    }

    private static int findFreePort() {
        new ServerSocket(0).withCloseable { it.localPort }
    }
}
