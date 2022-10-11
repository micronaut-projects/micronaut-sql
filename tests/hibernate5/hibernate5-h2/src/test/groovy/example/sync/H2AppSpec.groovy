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

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import io.micronaut.test.support.TestPropertyProvider

@MicronautTest
class H2AppSpec extends AbstractAppSpec implements TestPropertyProvider {

    @Override
    Map<String, String> getProperties() {
        return [
                "jpa.default.properties.hibernate.dialect"  : "org.hibernate.dialect.H2Dialect",
                "jpa.default.compile-time-hibernate-proxies": "true",
                "datasources.default.url"                   : "jdbc:h2:mem:devDb;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE",
                "datasources.default.driverClassName"       : "org.h2.Driver",
                "datasources.default.username"              : "sa",
                "datasources.default.password"              : ""
        ]
    }
}
