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
package io.micronaut.configuration.jasync.health

import io.micronaut.context.ApplicationContext
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.management.endpoint.health.HealthEndpoint
import io.micronaut.management.health.indicator.HealthResult
import io.micronaut.runtime.server.EmbeddedServer
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification

class JasyncHealthIndicatorSpec extends Specification {

    void "test jasync health indicator"() {
        given:
        PostgreSQLContainer postgres = new PostgreSQLContainer()
        postgres.start()
        EmbeddedServer embeddedServer = ApplicationContext.run(EmbeddedServer, [
                'jasync.client.port'                        : postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                'jasync.client.host'                        : postgres.getContainerIpAddress(),
                'jasync.client.database'                    : postgres.databaseName,
                'jasync.client.username'                    : postgres.username,
                'jasync.client.password'                    : postgres.password,
                (HealthEndpoint.PREFIX + '.enabled')        : true,
                (HealthEndpoint.PREFIX + '.jasync.enabled') : true,
                (HealthEndpoint.PREFIX + '.sensitive')      : false,
                (HealthEndpoint.PREFIX + '.url.enabled')    : true,
                (HealthEndpoint.PREFIX + '.details-visible'): 'ANONYMOUS',
        ]
        )
        embeddedServer.start()

        URL server = embeddedServer.getURL()
        RxHttpClient rxClient = embeddedServer.applicationContext.createBean(RxHttpClient, server)

        when:
        def response = rxClient.toBlocking().exchange('/health', HealthResult.class)

        then:
        response.code() == HttpStatus.OK.code
        response.body().status.name == "UP"
        response.body().details.toString().contains("PostgreSQL ${postgres.DEFAULT_TAG}")

        when:
        postgres.stop()
        rxClient.toBlocking().exchange('/health')

        then:
        def t = thrown(HttpClientResponseException)
        t.status == HttpStatus.SERVICE_UNAVAILABLE

        cleanup:
        embeddedServer?.stop()
        postgres?.stop()
    }

}
