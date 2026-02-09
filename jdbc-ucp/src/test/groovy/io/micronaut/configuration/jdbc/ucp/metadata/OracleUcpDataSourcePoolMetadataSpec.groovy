/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.configuration.jdbc.ucp.metadata

import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.http.HttpStatus
import io.micronaut.http.client.HttpClient
import io.micronaut.runtime.server.EmbeddedServer
import oracle.ucp.UniversalConnectionPool
import oracle.ucp.admin.UniversalConnectionPoolManager
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.AutoCleanup
import spock.lang.Shared

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED

class OracleUcpDataSourcePoolMetadataSpec extends AbstractDataSourcePoolMetadataSpec {

    @Shared
    @AutoCleanup
    ApplicationContext context = ApplicationContext.run(MapPropertySource.of(
            this.class.getSimpleName(),
            ['datasources.default'                        : [:],
             'datasources.foo'                            : [:],
             'endpoints.metrics.sensitive'                : false,
             (MICRONAUT_METRICS_ENABLED)                  : true,
             (MICRONAUT_METRICS_BINDERS + ".jdbc.enabled"): true]
    ), this.class.getSimpleName())

    @Shared
    @AutoCleanup
    EmbeddedServer embeddedServer = context.getBean(EmbeddedServer).start()

    @Shared
    @AutoCleanup
    HttpClient httpClient = context.createBean(HttpClient, embeddedServer.getURL())

    def "test wire class manually"() {
        given:
        UniversalConnectionPoolManager poolManager = Mock(UniversalConnectionPoolManager)
        UniversalConnectionPool pool = Mock(UniversalConnectionPool)
        PoolDataSource dataSource = Mock(PoolDataSource)

        when:
        def metadata = new OracleUcpDataSourcePoolMetadata(dataSource, poolManager)
        metadata.getActive() // pool access happens lazily

        then:
        1 * dataSource.getConnectionPoolName() >> "JDBC_UCP"
        1 * poolManager.getConnectionPool("JDBC_UCP") >> pool
        metadata
        metadata.getActive() == null
        metadata.getDefaultAutoCommit() == false
        metadata.getIdle() == null
        metadata.getMax() == null
        metadata.getMin() == null
    }

    def "check metrics endpoint for datasource metrics for #metric"() {
        when:
        def response = httpClient.toBlocking().exchange("/metrics", Map)
        Map result = response.body()

        then:
        response.code() == HttpStatus.OK.code
        result.names.contains(metric)

        where:
        metric << metricNames

    }

    def "check metrics endpoint for datasource metrics #metric"() {
        when:
        def response = httpClient.toBlocking().exchange("/metrics/$metric", Map)
        Map result = (Map) response.body()

        then:
        response.code() == HttpStatus.OK.code
        result.name == metric

        when:
        def tags = result.availableTags.findAll {
            it.tag == 'name'
        }

        then:
        tags

        and:
        tags.each { Map tag ->
            assert tag.values.contains('default')
            assert tag.values.contains('foo')
        }

        where:
        metric << metricNames
    }

}
