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

package io.micronaut.configuration.vertx.mysql.client.health

import io.micronaut.context.ApplicationContext
import io.micronaut.health.HealthStatus
import io.micronaut.management.health.indicator.HealthResult
import io.reactivex.Flowable
import org.testcontainers.containers.MySQLContainer
import spock.lang.Specification

class MysqlPoolHealthIndicatorSpec extends Specification{
    void "test vertx-mysql-client health indicator"() {
        given:
        MySQLContainer mysql = new MySQLContainer()
        mysql.start()
        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.mysql.client.port': mysql.getMappedPort(MySQLContainer.MYSQL_PORT),
                'vertx.mysql.client.host': mysql.getContainerIpAddress(),
                'vertx.mysql.client.database': mysql.databaseName,
                'vertx.mysql.client.user': mysql.username,
                'vertx.mysql.client.password': mysql.password,
                'vertx.mysql.client.pool.maxSize': '5'
        )

        when:
        MysqlHealthIndicator indicator = applicationContext.getBean(MysqlHealthIndicator)
        HealthResult result = Flowable.fromPublisher(indicator.getResult()).blockingFirst()

        then:
        result.status == HealthStatus.UP
        result.details.version.startsWith("${mysql.DEFAULT_TAG}".toString())

        when:
        mysql.stop()
        result = Flowable.fromPublisher(indicator.getResult()).blockingFirst()

        then:
        result.status == HealthStatus.DOWN


        cleanup:
        applicationContext?.stop()
    }

}
