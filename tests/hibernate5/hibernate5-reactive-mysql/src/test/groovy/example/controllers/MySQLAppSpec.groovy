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
package example.controllers

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.MySQLContainer
import org.testcontainers.utility.DockerImageName

@MicronautTest(packages = "example.domain", transactional = false)
class MySQLAppSpec extends AbstractDBContainerAppSpec implements TestPropertyProvider {

    Map<String, String> provideProperties(JdbcDatabaseContainer databaseContainer) {
        [
                'vertx.mysql.client.port'                 : databaseContainer.getMappedPort(MySQLContainer.MYSQL_PORT),
                'vertx.mysql.client.host'                 : databaseContainer.getHost(),
                'vertx.mysql.client.database'             : databaseContainer.databaseName,
                'vertx.mysql.client.user'                 : databaseContainer.username,
                'vertx.mysql.client.password'             : databaseContainer.password,
                'vertx.mysql.client.maxSize'              : '5',
                "jpa.default.properties.hibernate.dialect": 'org.hibernate.dialect.MySQL8Dialect',
                "jpa.default.reactive"                    : 'true'

//                "jpa.default.properties.hibernate.connection.url"     : databaseContainer.getJdbcUrl(),
//                "jpa.default.properties.hibernate.connection.username": databaseContainer.getUsername(),
//                "jpa.default.properties.hibernate.connection.password": databaseContainer.getPassword(),
        ] as Map<String, String>
    }

    @Override
    JdbcDatabaseContainer getJdbcDatabaseContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.0.11"))
    }
}
