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
package io.micronaut.configuration.jdbc.hikari

import com.zaxxer.hikari.metrics.micrometer.MicrometerMetricsTrackerFactory
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.BeanCreatedEvent
import io.micronaut.jdbc.DataSourceResolver
import spock.lang.Specification

import javax.sql.DataSource

import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_BINDERS
import static io.micronaut.configuration.metrics.micrometer.MeterRegistryFactory.MICRONAUT_METRICS_ENABLED

class HikariMetricsConfigurerSpec extends Specification {

    void "each pool of the factory reports to the meter registry"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default': [:],
                'datasources.foo'    : [:],
        ])
        MeterRegistry meterRegistry = applicationContext.getBean(MeterRegistry)

        expect:
        ['default', 'foo'].every { name ->
            HikariUrlDataSource pool = hikari(applicationContext, name)
            pool.metricRegistry.is(meterRegistry)
        }
        meterRegistry.find("hikaricp.connections").gauges().size() == 2

        cleanup:
        applicationContext.close()
    }

    void "the pools do not report when the JDBC metrics binder is disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default'                        : [:],
                (MICRONAUT_METRICS_BINDERS + ".jdbc.enabled"): false,
        ])

        expect:
        hikari(applicationContext, 'default').metricRegistry == null
        applicationContext.getBean(MeterRegistry).find("hikaricp.connections").gauges().isEmpty()

        cleanup:
        applicationContext.close()
    }

    void "the pools do not report when metrics are disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default'      : [:],
                (MICRONAUT_METRICS_ENABLED): false,
        ])

        expect:
        !applicationContext.containsBean(MeterRegistry)
        !applicationContext.containsBean(HikariMetricsConfigurer)
        hikari(applicationContext, 'default').metricRegistry == null

        cleanup:
        applicationContext.close()
    }

    void "a pool that already reports keeps its registry when the listener runs on it again, as it does on a pool retained across a restart"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(['datasources.default': [:]])
        MeterRegistry meterRegistry = applicationContext.getBean(MeterRegistry)
        DataSource dataSource = applicationContext.getBean(DataSource)
        HikariUrlDataSource pool = hikari(applicationContext, 'default')
        BeanCreatedEvent<DataSource> event = Stub(BeanCreatedEvent) {
            getBean() >> dataSource
        }

        when: "the listener of this context runs again, then the listener of a next context, with a registry of its own"
        def again = applicationContext.getBean(HikariMetricsConfigurer).onCreated(event)
        def next = new HikariMetricsConfigurer(new SimpleMeterRegistry(), null, true, applicationContext.environment).onCreated(event)

        then: "Hikari, which accepts a registry once, is not given another, and the pool is registered once"
        noExceptionThrown()
        again.is(dataSource)
        next.is(dataSource)
        pool.metricRegistry.is(meterRegistry)
        meterRegistry.find("hikaricp.connections").gauges().size() == 1

        cleanup:
        applicationContext.close()
    }

    void "a pool given a metrics tracker factory of its own keeps it"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default'                        : [:],
                (MICRONAUT_METRICS_BINDERS + ".jdbc.enabled"): false,
        ])
        DataSource dataSource = applicationContext.getBean(DataSource)
        HikariUrlDataSource pool = hikari(applicationContext, 'default')
        def trackerFactory = new MicrometerMetricsTrackerFactory(new SimpleMeterRegistry())
        pool.metricsTrackerFactory = trackerFactory
        BeanCreatedEvent<DataSource> event = Stub(BeanCreatedEvent) {
            getBean() >> dataSource
        }

        when:
        new HikariMetricsConfigurer(new SimpleMeterRegistry(), null, true, applicationContext.environment).onCreated(event)

        then:
        noExceptionThrown()
        pool.metricRegistry == null
        pool.metricsTrackerFactory.is(trackerFactory)

        cleanup:
        applicationContext.close()
    }

    void "in development mode a pool that the listener of a next context runs on reports to the next registry and drops the previous one"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default'  : [:],
                'micronaut.dev.enabled': true,
        ])
        MeterRegistry first = applicationContext.getBean(MeterRegistry)
        DataSource dataSource = applicationContext.getBean(DataSource)
        HikariUrlDataSource pool = hikari(applicationContext, 'default')
        BeanCreatedEvent<DataSource> event = Stub(BeanCreatedEvent) {
            getBean() >> dataSource
        }
        MeterRegistry second = new SimpleMeterRegistry()
        def next = new HikariMetricsConfigurer(second, null, true, applicationContext.environment)

        expect: "the pool reports to the first registry, through a tracker that keeps no registry in the pool configuration"
        pool.metricRegistry == null
        first.find("hikaricp.connections").gauges().size() == 1

        when: "the first context stops using the pool, and a next context is served it"
        applicationContext.getBean(HikariMetricsConfigurer).unbind()
        next.onCreated(event)
        dataSource.connection.close()

        then: "the pool reports to the next registry only, and no longer holds the first"
        first.find("hikaricp.connections").gauges().isEmpty()
        second.find("hikaricp.connections").gauges().size() == 1
        second.find("hikaricp.connections.usage").timer().count() == 1
        ((RebindableMetricsTrackerFactory) pool.metricsTrackerFactory).registry().is(second)

        when: "the next context stops"
        next.unbind()

        then:
        second.find("hikaricp.connections").gauges().isEmpty()
        ((RebindableMetricsTrackerFactory) pool.metricsTrackerFactory).registry() == null

        cleanup:
        applicationContext.close()
    }

    void "in development mode the registry of a next context that is served the pool first is kept when the previous context stops"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run([
                'datasources.default'  : [:],
                'micronaut.dev.enabled': true,
        ])
        DataSource dataSource = applicationContext.getBean(DataSource)
        HikariUrlDataSource pool = hikari(applicationContext, 'default')
        BeanCreatedEvent<DataSource> event = Stub(BeanCreatedEvent) {
            getBean() >> dataSource
        }
        MeterRegistry second = new SimpleMeterRegistry()

        when:
        new HikariMetricsConfigurer(second, null, true, applicationContext.environment).onCreated(event)
        applicationContext.getBean(HikariMetricsConfigurer).unbind()

        then:
        second.find("hikaricp.connections").gauges().size() == 1
        ((RebindableMetricsTrackerFactory) pool.metricsTrackerFactory).registry().is(second)

        cleanup:
        applicationContext.close()
    }

    private static HikariUrlDataSource hikari(ApplicationContext applicationContext, String name) {
        DataSourceResolver resolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)
        return (HikariUrlDataSource) resolver.resolve(applicationContext.getBean(DataSource, io.micronaut.inject.qualifiers.Qualifiers.byName(name)))
    }
}
