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

import io.micronaut.test.support.TestPropertyProvider
import org.testcontainers.containers.JdbcDatabaseContainer
import spock.lang.AutoCleanup
import spock.lang.Shared

abstract class AbstractDBContainerAppSpec extends AbstractAppSpec implements TestPropertyProvider {

    @Shared
    @AutoCleanup
    JdbcDatabaseContainer dbContainer = getJdbcDatabaseContainer()

    @Override
    Map<String, String> getProperties() {
        dbContainer.start()
        return [
                "datasources.default.url"                   : dbContainer.getJdbcUrl(),
                "jpa.default.compile-time-hibernate-proxies": true,
                "jpa.default.properties.hibernate.dialect"  : getHibernateDialect(),
                "datasources.default.driverClassName"       : dbContainer.getDriverClassName(),
                "datasources.default.username"              : dbContainer.getUsername(),
                "datasources.default.password"              : dbContainer.getPassword(),
        ]
    }

    abstract JdbcDatabaseContainer getJdbcDatabaseContainer();

    abstract String getHibernateDialect();

}
