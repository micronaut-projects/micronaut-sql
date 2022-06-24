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
package example.sync

import io.micronaut.test.support.TestPropertyProvider
import org.testcontainers.containers.JdbcDatabaseContainer
import spock.lang.AutoCleanup
import spock.lang.Shared

import java.time.Duration

abstract class AbstractR2DBCContainerAppSpec extends AbstractAppSpec implements TestPropertyProvider {

    @Shared
    @AutoCleanup
    JdbcDatabaseContainer dbContainer = getJdbcDatabaseContainer()

    @Override
    Map<String, String> getProperties() {
        dbContainer.start()
        def properties = provideProperties(dbContainer)
        return properties
    }

    Map<String, String> provideProperties(JdbcDatabaseContainer container) {
        def driverName = driverName()
        return [
                "r2dbc.datasources.default.username"                : container == null ? "" : container.getUsername(),
                "r2dbc.datasources.default.password"                : container == null ? "" : container.getPassword(),
                "r2dbc.datasources.default.url"                     : "r2dbc:postgres://${getR2dbUrlSuffix(driverName, container)}",
                "r2dbc.datasources.default.options.connectTimeout"  : Duration.ofMinutes(1).toString(),
                "r2dbc.datasources.default.options.statementTimeout": Duration.ofMinutes(1).toString(),
                "r2dbc.datasources.default.options.lockTimeout"     : Duration.ofMinutes(1).toString(),
                "r2dbc.datasources.default.options.protocol"        : driverName,
                "r2dbc.datasources.default.options.driver"          : 'pool'
        ] as Map<String, String>
    }

    abstract JdbcDatabaseContainer getJdbcDatabaseContainer();

    String getR2dbUrlSuffix(String driverName, JdbcDatabaseContainer container) {
        switch (driverName) {
            case "postgresql":
                return "${container.getHost()}:${container.getFirstMappedPort()}/${container.getDatabaseName()}?options=statement_timeout=5s"
            case "h2":
                return "/testdb"
            case "sqlserver":
                return "${container.getHost()}:${container.getFirstMappedPort()}"
            case "oracle":
                return "${container.getHost()}:${container.getFirstMappedPort()}/xe"
            case "mariadb":
            case "mysql":
                return "${container.getUsername()}:${container.getPassword()}@${container.getHost()}:${container.getFirstMappedPort()}/${container.getDatabaseName()}"
        }
    }

    abstract String driverName()
}
