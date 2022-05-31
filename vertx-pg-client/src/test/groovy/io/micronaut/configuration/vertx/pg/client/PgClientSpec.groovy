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
package io.micronaut.configuration.vertx.pg.client

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

import io.vertx.reactivex.pgclient.PgPool
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowIterator
import io.vertx.reactivex.sqlclient.RowSet
import org.testcontainers.containers.PostgreSQLContainer

//tag::pg-testcontainer-import[]

import spock.lang.AutoCleanup

//end::pg-testcontainer-import[]

import spock.lang.Shared
import spock.lang.Specification


class PgClientSpec extends Specification {

    // tag::pg-testcontainer[]
    @Shared @AutoCleanup PostgreSQLContainer postgres = new PostgreSQLContainer()

    // end::pg-testcontainer[]

    //tag::pg-dbstats[]
    void "test a simple query for database stats"() {
        given:
        //tag::pg-client-conf[]
        postgres.start()

        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.pg.client.port': postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                'vertx.pg.client.host': postgres.getHost(),
                'vertx.pg.client.database': postgres.databaseName,
                'vertx.pg.client.user': postgres.username,
                'vertx.pg.client.password': postgres.password,
                'vertx.pg.client.maxSize': '5'
        )

        //end::pg-client-conf[]
        String result

        when:

        // tag::pgPool-bean[]
        PgPool client = applicationContext.getBean(PgPool)
        // end::pgPool-bean[]

        // tag::query[]
        result = client.query('SELECT * FROM pg_stat_database').rxExecute().map({ RowSet<Row> rowSet -> // <1>
            int size = 0
            RowIterator<Row> iterator = rowSet.iterator()
            while (iterator.hasNext()) {
                iterator.next()
                size++
            }
            return "Size: ${size}"
        }).blockingGet()
        // end::query[]

        then:
        result == "Size: 4"

        cleanup:
        client.close()
        postgres.stop()
    }
    //end::pg-dbstats[]

}
