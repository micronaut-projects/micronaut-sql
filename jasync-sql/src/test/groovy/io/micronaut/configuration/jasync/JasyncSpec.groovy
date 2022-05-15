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
package io.micronaut.configuration.jasync

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.ResultSet

/*
 * Copyright 2017-2018 original authors
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

//tag::appcontext-import[]
import io.micronaut.context.ApplicationContext
//end::appcontext-import[]
//tag::pg-testcontainer-import[]
import org.testcontainers.containers.PostgreSQLContainer
//end::pg-testcontainer-import[]
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

/**
 * @author puneetbehl
 * @since 1.0
 */
class JasyncSpec extends Specification {

    // tag::pg-testcontainer[]
    @Shared @AutoCleanup PostgreSQLContainer postgres = new PostgreSQLContainer()

    // end::pg-testcontainer[]

    //tag::pg-dbstats[]
    void "test a simple query for database stats"() {
        given:
        //tag::pg-client-conf[]
        postgres.start()

        ApplicationContext applicationContext = ApplicationContext.run(
                'jasync.client.port': postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                'jasync.client.host': postgres.getHost(),
                'jasync.client.database': postgres.databaseName,
                'jasync.client.username': postgres.username,
                'jasync.client.password': postgres.password
        )

        //end::pg-client-conf[]
        String result

        when:

        // tag::pgPool-bean[]
        Connection client = applicationContext.getBean(Connection)
        // end::pgPool-bean[]

        // tag::query[]
        result = client.sendQuery('SELECT * FROM pg_stat_database').thenApply({ QueryResult resultSet -> // <1>
            return "Size: ${resultSet.rows.size()}"
        }).get()
        // end::query[]

        then:
        result == "Size: 4"

        cleanup:
        client.disconnect()
        postgres.stop()
    }
    //end::pg-dbstats[]

}
