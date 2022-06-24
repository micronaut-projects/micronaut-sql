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

import example.sync.AbstractAppSpec
import io.micronaut.test.support.TestPropertyProvider
import org.testcontainers.containers.JdbcDatabaseContainer
import spock.lang.AutoCleanup
import spock.lang.Shared

abstract class AbstractHibernateReactiveAppSpec extends AbstractAppSpec implements TestPropertyProvider {

    @Shared
    @AutoCleanup
    JdbcDatabaseContainer dbContainer = getJdbcDatabaseContainer()

    @Override
    Map<String, String> getProperties() {
        dbContainer.start()
        return provideProperties(dbContainer)
    }

    Map<String, String> provideProperties(JdbcDatabaseContainer databaseContainer) {
        [
                "jpa.default.properties.hibernate.connection.url"     : databaseContainer.getJdbcUrl(),
                "jpa.default.properties.hibernate.connection.username": databaseContainer.getUsername(),
                "jpa.default.properties.hibernate.connection.password": databaseContainer.getPassword(),
                "jpa.default.reactive"                                : 'true'
        ]
    }

    abstract JdbcDatabaseContainer getJdbcDatabaseContainer();

}
