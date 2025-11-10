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
package io.micronaut.configuration.vertx.mysql.client

//tag::appcontext-import[]
import io.micronaut.context.ApplicationContext
//end::appcontext-import[]

import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowIterator
import io.vertx.sqlclient.RowSet

//tag::mysql-testcontainer-import[]
import org.testcontainers.containers.MySQLContainer
//end::mysql-testcontainer-import[]
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class MySQLClientSpec extends Specification{
    // tag::mysql-testcontainer[]
    @Shared @AutoCleanup MySQLContainer mysql = new MySQLContainer("mysql:8.4.5")

    // end::mysql-testcontainer[]

    //tag::mysql-dbstats[]
    void "test a simple query for database stats"() {
        given:
        //tag::mysql-client-conf[]
        mysql.start()

        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.mysql.client.port': mysql.getMappedPort(MySQLContainer.MYSQL_PORT),
                'vertx.mysql.client.host': mysql.getHost(),
                'vertx.mysql.client.database': mysql.databaseName,
                'vertx.mysql.client.user': mysql.username,
                'vertx.mysql.client.password': mysql.password,
                'vertx.mysql.client.maxSize': '5'
        )

        //end::mysql-client-conf[]
        String result

        when:

        // tag::mysqlPool-bean[]
        Pool client = applicationContext.getBean(Pool)
        // end::mysqlPool-bean[]

        //
        client.query("CREATE TABLE IF NOT EXISTS foo(id INTEGER)").execute().toCompletionStage().toCompletableFuture().get()
        client.query("INSERT INTO foo(id) VALUES (0);").execute().toCompletionStage().toCompletableFuture().get()

        // tag::query[]
        RowSet<Row> rowSet = client.query('SELECT * FROM foo').execute().toCompletionStage().toCompletableFuture().get() // <1>
        RowIterator<Row> iterator = rowSet.iterator()
        int id = iterator.next().getInteger("id")
        result = "id: ${id}"
        // end::query[]

        then:
        result == "id: 0"

        cleanup:
        client.close()
        mysql.stop()
    }
    //end::mysql-dbstats[]

}
