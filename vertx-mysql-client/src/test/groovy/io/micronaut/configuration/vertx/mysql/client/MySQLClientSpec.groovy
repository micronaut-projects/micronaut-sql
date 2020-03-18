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

import io.vertx.reactivex.mysqlclient.MySQLPool
import io.vertx.reactivex.sqlclient.Row
import io.vertx.reactivex.sqlclient.RowIterator
import io.vertx.reactivex.sqlclient.RowSet

//tag::mysql-testcontainer-import[]
import org.testcontainers.containers.MySQLContainer
//end::mysql-testcontainer-import[]
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

class MySQLClientSpec extends Specification{
    // tag::mysql-testcontainer[]
    @Shared @AutoCleanup MySQLContainer mysql = new MySQLContainer()

    // end::mysql-testcontainer[]

    //tag::mysql-dbstats[]
    void "test a simple query for database stats"() {
        given:
        //tag::mysql-client-conf[]
        mysql.start()

        ApplicationContext applicationContext = ApplicationContext.run(
                'vertx.mysql.client.port': mysql.getMappedPort(MySQLContainer.MYSQL_PORT),
                'vertx.mysql.client.host': mysql.getContainerIpAddress(),
                'vertx.mysql.client.database': mysql.databaseName,
                'vertx.mysql.client.user': mysql.username,
                'vertx.mysql.client.password': mysql.password,
                'vertx.mysql.client.maxSize': '5'
        )

        //end::mysql-client-conf[]
        String result

        when:

        // tag::mysqlPool-bean[]
        MySQLPool client = applicationContext.getBean(MySQLPool)
        // end::mysqlPool-bean[]

        //
        client.rxQuery("CREATE TABLE IF NOT EXISTS foo(id INTEGER)").blockingGet()
        client.rxQuery("INSERT INTO foo(id) VALUES (0);").blockingGet()

        // tag::query[]
        result = client.rxQuery('SELECT * FROM foo').map({ RowSet<Row> rowSet -> // <1>
            RowIterator<Row> iterator = rowSet.iterator()
            int id = iterator.next().getInteger("id")
            return "id: ${id}"
        }).blockingGet()
        // end::query[]

        then:
        result == "id: 0"

        cleanup:
        client.close()
        mysql.stop()
    }
    //end::mysql-dbstats[]

}
