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
package example.jooq.reactive

import example.sync.AbstractR2DBCContainerAppSpec
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@MicronautTest(transactional = false)
class PostgresAppSpec extends AbstractR2DBCContainerAppSpec {

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

    @Override
    Map<String, String> provideProperties(JdbcDatabaseContainer databaseContainer) {
        super.provideProperties(databaseContainer) + [
                "jooq.r2dbc-datasources.default.sql-dialect": 'POSTGRES'
        ]
    }

    @Override
    String driverName() {
        return "postgresql"
    }
}
