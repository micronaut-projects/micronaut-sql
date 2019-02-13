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

package io.micronaut.configuration.jasync.health

import io.micronaut.context.ApplicationContext
import io.micronaut.health.HealthStatus

import io.micronaut.management.health.indicator.HealthResult
import io.reactivex.Flowable
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification

import java.util.concurrent.TimeUnit


class JasyncHealthIndicatorSpec extends Specification {

    void "test jasync health indicator"() {
        given:
        PostgreSQLContainer postgres = new PostgreSQLContainer()
        postgres.start()
        ApplicationContext applicationContext = ApplicationContext.run(
                'jasync.client.port': postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                'jasync.client.host': postgres.getContainerIpAddress(),
                'jasync.client.database': postgres.databaseName,
                'jasync.client.username': postgres.username,
                'jasync.client.password': postgres.password
        )

        when:
        JaysncHealthIndicator indicator = applicationContext.getBean(JaysncHealthIndicator)
        HealthResult result = Flowable.fromPublisher(indicator.getResult()).timeout(10, TimeUnit.SECONDS).blockingFirst()

        then:
        result.status == HealthStatus.UP
        result.details.version.startsWith("PostgreSQL ${postgres.DEFAULT_TAG}".toString())

        when:
        postgres.stop()
        result = Flowable.fromPublisher(indicator.getResult()).timeout(10, TimeUnit.SECONDS).blockingFirst()

        then:
        result.status == HealthStatus.DOWN


        cleanup:
        applicationContext?.stop()
    }

}
