package io.micronaut.configuration.postgres.reactive

/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import io.vertx.reactivex.sqlclient.RowIterator
import io.vertx.reactivex.sqlclient.RowSet

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
class PostgresReactiveSpec extends Specification {

    // tag::pg-testcontainer[]
    @Shared @AutoCleanup PostgreSQLContainer postgres = new PostgreSQLContainer()

    // end::pg-testcontainer[]

    //tag::pg-dbstats[]
    void "test a simple query for database stats"() {
        given:
        //tag::pg-client-conf[]
        postgres.start()

        ApplicationContext applicationContext = ApplicationContext.run(
                'postgres.reactive.client.port': postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                'postgres.reactive.client.host': postgres.getContainerIpAddress(),
                'postgres.reactive.client.database': postgres.databaseName,
                'postgres.reactive.client.user': postgres.username,
                'postgres.reactive.client.password': postgres.password,
                'postgres.reactive.pool.maxSize': '5'
        )

        //end::pg-client-conf[]
        String result

        when:

        // tag::pgPool-bean[]
        PgPool client = applicationContext.getBean(PgPool)
        // end::pgPool-bean[]

        // tag::query[]
        result = client.rxQuery('SELECT * FROM pg_stat_database').map({ RowSet pgRowSet -> // <1>
            int size = 0
            RowIterator iterator = pgRowSet.iterator()
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
