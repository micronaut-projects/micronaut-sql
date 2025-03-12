package io.micronaut.configuration.jdbi.example

import io.micronaut.configuration.jdbi.example.jdbitransaction.ConcurrentTransactionsBug
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import spock.lang.Specification

class ApplicationSpec extends Specification {

    def "test Demonstrates stealing of transaction between concurrent threads"() {
        given:
            ApplicationContext applicationContext = new DefaultApplicationContext("test")
            applicationContext.environment.addPropertySource(MapPropertySource.of(
                    'test',
                    ['datasources.default': [:]]
            ))
            applicationContext.start()

        when:
            def dbSetup = applicationContext.getBean(DatabaseSetup)
            def bugService = applicationContext.getBean(ConcurrentTransactionsBug)
            dbSetup.initialize()
            bugService.connectionStatusLostDuringExecution()
        then:
            noExceptionThrown()

        cleanup:
            dbSetup.drop()
            applicationContext.close()
    }

    def "test Demonstrates lost connection on explicit transaction"() {
        given:
            ApplicationContext applicationContext = new DefaultApplicationContext("test")
            applicationContext.environment.addPropertySource(MapPropertySource.of(
                    'test',
                    ['datasources.default': [:]]
            ))
            applicationContext.start()

        when:
            def dbSetup = applicationContext.getBean(DatabaseSetup)
            def bugService = applicationContext.getBean(ConcurrentTransactionsBug)
            dbSetup.initialize()
            dbSetup.fillInitialRecords()
            bugService.transactionStealedFromOtherThread()
        then:
            noExceptionThrown()

        cleanup:
            dbSetup.drop()
            applicationContext.close()
    }
}
