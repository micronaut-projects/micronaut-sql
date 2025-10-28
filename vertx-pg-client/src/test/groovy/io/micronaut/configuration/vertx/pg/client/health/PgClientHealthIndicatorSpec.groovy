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
package io.micronaut.configuration.vertx.pg.client.health

import io.micronaut.context.ApplicationContext
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import java.util.concurrent.CompletableFuture
import org.reactivestreams.Subscriber
import org.reactivestreams.Subscription
import org.testcontainers.containers.PostgreSQLContainer
import spock.lang.Specification


class PgClientHealthIndicatorSpec extends Specification {

    void "test vertx-pg-client health indicator"() {
        given:
        PostgreSQLContainer postgres = new PostgreSQLContainer("postgres")
        postgres.start()
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.pg.client.port': postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                'vertx.pg.client.host': postgres.getHost(),
                'vertx.pg.client.database': postgres.databaseName,
                'vertx.pg.client.user': postgres.username,
                'vertx.pg.client.password': postgres.password,
                'vertx.pg.client.maxSize': '5'
        )

        when:
        PgHealthIndicator indicator = applicationContext.getBean(PgHealthIndicator)
        HealthResult result
        int attempts = 0
        while (true) {
            CompletableFuture<HealthResult> future1 = new CompletableFuture<>()
            indicator.getResult().subscribe(new Subscriber<HealthResult>() {
                @Override
                void onSubscribe(Subscription s) { s.request(1) }
                @Override
                void onNext(HealthResult hr) { future1.complete(hr) }
                @Override
                void onError(Throwable t) { future1.completeExceptionally(t) }
                @Override
                void onComplete() { }
            })
            try {
                result = future1.get()
                if (result.status == HealthStatus.UP) {
                    break
                }
            } catch (Throwable t) {
                // ignore and retry
            }
            attempts++
            if (attempts >= 10) {
                // give up after retries; assertion will fail if not UP
                break
            }
            Thread.sleep(200)
        }

        then:
        result.status == HealthStatus.UP
        result.details.version.startsWith("PostgreSQL ")

        when:
        postgres.stop()
        CompletableFuture<HealthResult> future2 = new CompletableFuture<>()
        indicator.getResult().subscribe(new Subscriber<HealthResult>() {
            @Override
            void onSubscribe(Subscription s) { s.request(1) }
            @Override
            void onNext(HealthResult hr) { future2.complete(hr) }
            @Override
            void onError(Throwable t) { future2.completeExceptionally(t) }
            @Override
            void onComplete() { }
        })
        result = future2.get()

        then:
        result.status == HealthStatus.DOWN


        cleanup:
        applicationContext?.stop()
    }

}
