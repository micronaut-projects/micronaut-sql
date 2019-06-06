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
package io.micronaut.configuration.jooq

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import org.jooq.Configuration
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.TransactionalRunnable
import org.jooq.impl.DSL
import spock.lang.Specification

import javax.sql.DataSource

class DslContextSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(DataSource)
        !applicationContext.containsBean(Configuration)
        !applicationContext.containsBean(DSLContext)

        cleanup:
        applicationContext.close()
    }

    void "test blank configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()

        expect:
        applicationContext.containsBean(DataSource)
        applicationContext.containsBean(Configuration)
        applicationContext.containsBean(DSLContext)

        when:
        Configuration configuration = applicationContext.getBean(DSLContext).configuration()

        then:
        configuration.dialect() == SQLDialect.H2
        configuration.executeListenerProviders().any {
            provider -> JooqExceptionTranslatorProvider.isInstance(provider)
        }
        SpringTransactionProvider.isInstance(configuration.transactionProvider())

        cleanup:
        applicationContext.close()
    }

    void "test sql dialect override"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default'     : [:],
                 'jooq.datasources.default.sql-dialect': 'POSTGRES']
        ))
        applicationContext.start()

        when:
        Configuration configuration = applicationContext.getBean(DSLContext).configuration()

        then:
        configuration.dialect() == SQLDialect.POSTGRES

        cleanup:
        applicationContext.close()
    }

    void "test simple sql"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        DSLContext db = applicationContext.getBean(DSLContext)
        int result = db.selectOne().fetchSingle(0, Integer)

        expect:
        result == 1

        cleanup:
        applicationContext.close()
    }

    void "test transaction sql"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        DSLContext db = applicationContext.getBean(DSLContext)
        TestService service = applicationContext.getBean(TestService)

        expect:
        service.count() == 1

        when:
        db.transaction((TransactionalRunnable) { conf ->
            DSL.using(conf).execute("DELETE FROM foo;")
            def count = DSL.using(conf).fetchCount(DSL.table("foo"))
            throw new RuntimeException("count=" + count)
        })

        then:
        RuntimeException ex = thrown()
        ex.message == "count=0"
        service.count() == 1

        when:
        service.abortTransaction()

        then:
        ex = thrown()
        ex.message == "count=2"
        service.count() == 1

        when:
        service.commitTransaction();

        then:
        service.count() == 2

        cleanup:
        applicationContext.close()
    }

}
