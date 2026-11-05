package io.micronaut.configuration.vertx.oracle.client.health

import io.micronaut.context.ApplicationContext
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import org.testcontainers.oracle.OracleContainer
import reactor.core.publisher.Mono
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class OracleHealthIndicatorSpec extends Specification {

    @Shared @AutoCleanup OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:23.26.1-faststart")

    def setupSpec() {
        oracle.start()
    }

    void "test the health indicator reports UP with the database version"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.host': oracle.host,
                'vertx.oracle.client.port': oracle.oraclePort,
                'vertx.oracle.client.service-name': oracle.databaseName,
                'vertx.oracle.client.user': oracle.username,
                'vertx.oracle.client.password': oracle.password
        )

        when:
        HealthResult result = Mono.from(applicationContext.getBean(OracleHealthIndicator).result).block()

        then:
        result.status == HealthStatus.UP
        result.name == OracleHealthIndicator.NAME
        ((Map) result.details).version.toString().startsWith("23.")

        cleanup:
        applicationContext?.close()
    }

    void "test the health indicator reports DOWN when the database is unreachable"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.host': oracle.host,
                'vertx.oracle.client.port': 1,
                'vertx.oracle.client.service-name': oracle.databaseName,
                'vertx.oracle.client.user': oracle.username,
                'vertx.oracle.client.password': oracle.password
        )

        when:
        HealthResult result = Mono.from(applicationContext.getBean(OracleHealthIndicator).result).block()

        then:
        result.status == HealthStatus.DOWN

        cleanup:
        applicationContext?.close()
    }

    void "test the health indicator can be disabled"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.oracle.client.host': oracle.host,
                'endpoints.health.vertx.oracle.client.enabled': 'false'
        )

        expect:
        !applicationContext.containsBean(OracleHealthIndicator)

        cleanup:
        applicationContext?.close()
    }
}
