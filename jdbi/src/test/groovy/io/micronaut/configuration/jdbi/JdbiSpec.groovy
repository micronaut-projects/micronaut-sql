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
package io.micronaut.configuration.jdbi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.core.version.SemanticVersion
import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.internal.OnDemandExtensions
import org.jdbi.v3.core.statement.SqlStatements
import org.jdbi.v3.sqlobject.SqlObjectFactory
import spock.lang.Requires
import spock.lang.Specification
import spock.util.environment.Jvm

import javax.sql.DataSource


class JdbiSpec extends Specification {

    void "test no configuration"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.start()

        expect: "No beans are created"
        !applicationContext.containsBean(DataSource)
        !applicationContext.containsBean(Jdbi)

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
        applicationContext.containsBean(Jdbi)
        applicationContext.containsBean(JdbiCustomizer)

        when:
        Jdbi jdbi = applicationContext.getBean(Jdbi)

        then:
        jdbi.config != null

        cleanup:
        applicationContext.close()
    }

    void "test plugins"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default'     : [:]]
        ))
        applicationContext.start()

        when:
        Jdbi jdbi = applicationContext.getBean(Jdbi)

        then:
        jdbi.getConfig(OnDemandExtensions).factory instanceof SqlObjectFactory

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
        TestService service = applicationContext.getBean(TestService)
        int result = service.count()

        expect:
        result == 1

        cleanup:
        cleanupDatabase(applicationContext.getBean(Jdbi))
        applicationContext.close()
    }

    // fails due to https://issues.apache.org/jira/browse/GROOVY-10145
    @Requires({
        SemanticVersion.isAtLeastMajorMinor(GroovySystem.version, 4, 0) ||
                !Jvm.current.isJava16Compatible()
    })
    void "test transaction sql"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        Jdbi jdbi = applicationContext.getBean(Jdbi)
        TestService service = applicationContext.getBean(TestService)

        when:
        jdbi.useTransaction { handle ->
            handle.execute("DELETE FROM foo")
            int count = handle.createQuery("SELECT COUNT(*) FROM foo").mapTo(Integer.class).one()
            throw new RuntimeException("count=" + count)
        }

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
        service.commitTransaction()

        then:
        service.count() == 2

        cleanup:
        cleanupDatabase(jdbi)
        applicationContext.close()
    }

    void "test jdbi transaction"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        Jdbi jdbi = applicationContext.getBean(Jdbi)
        TestService service = applicationContext.getBean(TestService)

        expect:
        service.count() == 1

        when:
        Integer count = jdbi.withExtension(TestInterface.class) { dao -> dao.count() }

        then:
        count == 1

        cleanup:
        cleanupDatabase(jdbi)
        applicationContext.close()
    }

    def "test jdbi customizer"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default': [:]]
        ))
        applicationContext.start()
        Jdbi jdbi = applicationContext.getBean(Jdbi)
        TestService service = applicationContext.getBean(TestService)

        expect:
        service.count() == 1

        when:
        String value = jdbi.getConfig(SqlStatements).getAttribute("test")

        then:
        value == "test"

        cleanup:
        cleanupDatabase(jdbi)
        applicationContext.close()
    }

    def "test jdbi customizer does not apply with wrong annotation name"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default2': [:]]
        ))
        applicationContext.start()
        Jdbi jdbi = applicationContext.getBean(Jdbi)
        TestService service = applicationContext.getBean(TestService)

        expect:
        service.count() == 1

        when:
        String value = jdbi.getConfig(SqlStatements).getAttribute("test")

        then:
        value == null

        cleanup:
        cleanupDatabase(jdbi)
        applicationContext.close()
    }

    private void cleanupDatabase(Jdbi jdbi) {
        def handle = jdbi.open()
        try {
            handle.execute("DELETE FROM foo")
        } finally {
            handle.close()
        }
    }

}
