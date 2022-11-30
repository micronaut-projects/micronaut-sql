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
package example.reactive

import example.reactive.AbstractHibernateReactiveAppSpec
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@MicronautTest(packages = "example.domain", transactional = false)
class PostgresAppSpec extends AbstractHibernateReactiveAppSpec {

    @Override
    Class<?> getOwnerClass() {
        Owner
    }

    @Override
    Class<?> getPetClass() {
        Pet
    }

    @Override
    JdbcDatabaseContainer getJdbcDatabaseContainer() {
        return new PostgreSQLContainer(DockerImageName.parse("postgres:10"))
    }

    Map<String, String> provideProperties(JdbcDatabaseContainer databaseContainer) {
        [
//                'vertx.pg.client.port'                    : databaseContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
//                'vertx.pg.client.host'                    : databaseContainer.getHost(),
//                'vertx.pg.client.database'                : databaseContainer.databaseName,
//                'vertx.pg.client.user'                    : databaseContainer.username,
//                'vertx.pg.client.password'                : databaseContainer.password,
//                'vertx.pg.client.maxSize'                 : '5',
//                "jpa.default.properties.hibernate.dialect": 'org.hibernate.dialect.PostgreSQL10Dialect',
                "jpa.default.reactive"                    : 'true',
                "jpa.default.properties.hibernate.connection.url"     : databaseContainer.getJdbcUrl(),
                "jpa.default.properties.hibernate.connection.username": databaseContainer.getUsername(),
                "jpa.default.properties.hibernate.connection.password": databaseContainer.getPassword(),
        ] as Map<String, String>
    }

}
