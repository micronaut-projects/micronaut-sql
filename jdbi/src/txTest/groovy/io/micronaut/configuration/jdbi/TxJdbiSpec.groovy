package io.micronaut.configuration.jdbi

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import io.micronaut.core.version.SemanticVersion
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.internal.OnDemandExtensions
import org.jdbi.v3.sqlobject.SqlObjectFactory
import spock.lang.IgnoreIf
import spock.lang.Requires
import spock.lang.Specification
import spock.util.environment.Jvm

import javax.sql.DataSource

class TxJdbiSpec extends Specification {

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
        TxTestService service = applicationContext.getBean(TxTestService)
        int result = service.count()

        expect:
        result == 1

        cleanup:
        service.dropData()
        applicationContext.close()
    }

    // fails due to https://issues.apache.org/jira/browse/GROOVY-10145
    @Requires({
        SemanticVersion.isAtLeastMajorMinor(GroovySystem.shortVersion, 4, 0) ||
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
        TxTestService service = applicationContext.getBean(TxTestService)

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
        service.dropData()
        applicationContext.close()
    }



}

