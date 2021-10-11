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
package io.micronaut.configuration.jdbc.hikari.metadata

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED

class HikariDataSourcePoolMetadataCustomPoolSpec extends Specification {

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run(MapPropertySource.of(
            'HikariDataSourcePoolMetadataCustomPoolSpec',
            ['datasources.default.poolName'               : 'DefaultPool',
             'datasources.foo.poolName'                   : 'FooPool',
             'endpoints.metrics.sensitive'                : false,
             (MICRONAUT_METRICS_ENABLED)                  : true,
             (MICRONAUT_METRICS_BINDERS + ".jdbc.enabled"): true]
    ), "HikariDataSourcePoolMetadataCustomPoolSpec")

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()

    @Shared
    @AutoCleanup
    HttpClient httpClient = context.createBean(HttpClient, embeddedServer.getURL())

    def "check metrics endpoint for datasource metrics"() {
        when:
        def response = httpClient.toBlocking().exchange("/metrics", Map)
        Map result = response.body()

        then:
        response.code() == HttpStatus.OK.code
        result.names.contains("hikaricp.connections.idle")
        result.names.contains("hikaricp.connections.pending")
        result.names.contains("hikaricp.connections")
        result.names.contains("hikaricp.connections.active")
        result.names.contains("hikaricp.connections.creation")
        result.names.contains("hikaricp.connections.max")
        result.names.contains("hikaricp.connections.min")
        result.names.contains("hikaricp.connections.usage")
        result.names.contains("hikaricp.connections.timeout")
        result.names.contains("hikaricp.connections.acquire")
    }

    @Unroll
    def "check metrics endpoint for datasource metrics #metric"() {
        when:
        def response = httpClient.toBlocking().exchange("/metrics/$metric", Map)
        Map result = (Map) response.body()

        then:
        response.code() == HttpStatus.OK.code
        result.name == metric

        when:
        def tags = result.availableTags.findAll {
            it.tag == 'pool'
        }

        then:
        tags

        and:
        tags.each { Map tag ->
            assert tag.values.contains('DefaultPool')
            assert tag.values.contains('FooPool')
        }

        where:
        metric << [
                'hikaricp.connections.idle',
                'hikaricp.connections.pending',
                'hikaricp.connections',
                'hikaricp.connections.active',
                'hikaricp.connections.creation',
                'hikaricp.connections.max',
                'hikaricp.connections.min',
                'hikaricp.connections.usage',
                'hikaricp.connections.timeout',
                'hikaricp.connections.acquire'
        ]
    }


}
