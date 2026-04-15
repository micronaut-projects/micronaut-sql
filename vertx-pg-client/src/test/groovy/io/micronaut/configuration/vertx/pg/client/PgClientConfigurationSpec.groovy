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
import io.vertx.sqlclient.Pool
import spock.lang.Specification

import java.net.ConnectException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit


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
        Throwable failure = connectFailure(applicationContext.getBean(Pool))

        then:
        rootCause(failure) instanceof ConnectException

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
        Throwable failure = connectFailure(applicationContext.getBean(Pool))

        then:
        rootCause(failure) instanceof ConnectException

        cleanup:
        applicationContext?.stop()
    }

    private static int findFreePort() {
        new ServerSocket(0).withCloseable { it.localPort }
    }

    private static Throwable connectFailure(Pool pool) {
        try {
            pool.getConnection().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS)
            return null
        } catch (ExecutionException e) {
            return e.cause
        } catch (TimeoutException e) {
            return e
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt()
            return e
        }
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable
        while (current?.cause != null) {
            current = current.cause
        }
        current
    }

}
