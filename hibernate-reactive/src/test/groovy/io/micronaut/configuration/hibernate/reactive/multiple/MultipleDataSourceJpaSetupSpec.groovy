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
package io.micronaut.configuration.hibernate.reactive.multiple

import io.micronaut.configuration.hibernate.reactive.multiple.other.Author
import io.micronaut.configuration.hibernate.reactive.multiple.xyz.Book
import io.micronaut.context.ApplicationContext
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.test.support.TestPropertyProvider
import org.hibernate.SessionFactory
import org.hibernate.reactive.stage.Stage
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class MultipleDataSourceJpaSetupSpec extends Specification implements TestPropertyProvider {

    @Shared
    @AutoCleanup
    JdbcDatabaseContainer dbContainer = new PostgreSQLContainer(DockerImageName.parse("postgres:9.6.12"))

    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(getProperties())

    @Override
    Map<String, String> getProperties() {
        dbContainer.start()
        [
                'datasources.default.url'                           : 'jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE',
                'jpa.default.packages-to-scan'                      : 'io.micronaut.configuration.hibernate.reactive.multiple.xyz',
                'jpa.other.packages-to-scan'                        : 'io.micronaut.configuration.hibernate.reactive.multiple.other',
                'jpa.other.properties.hibernate.hbm2ddl.auto'       : 'create-drop',
                "jpa.other.properties.hibernate.connection.url"     : dbContainer.getJdbcUrl(),
                "jpa.other.properties.hibernate.connection.username": dbContainer.getUsername(),
                "jpa.other.properties.hibernate.connection.password": dbContainer.getPassword(),
                "jpa.other.reactive"                                : 'true'
        ]
    }

    void "test multiple data sources setup"() {
        given:
            SessionFactory defaultSessionFactory = applicationContext.getBean(SessionFactory)
            SessionFactory otherSessionFactory = applicationContext.getBean(SessionFactory, Qualifiers.byName("other"))

        expect:
            defaultSessionFactory != otherSessionFactory
            defaultSessionFactory.getMetamodel().entity(Book)
            otherSessionFactory.getMetamodel().entity(Author)
            otherSessionFactory.unwrap(Stage.SessionFactory)
    }
}
