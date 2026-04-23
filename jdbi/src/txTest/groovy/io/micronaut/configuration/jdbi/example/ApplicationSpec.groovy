package io.micronaut.configuration.jdbi.example

import io.micronaut.configuration.jdbi.example.jdbitransaction.ExecutorTransactionIsolationService
import io.micronaut.configuration.jdbi.example.jdbitransaction.ConcurrentTransactionsBug
import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import spock.lang.Specification

class ApplicationSpec extends Specification {

    def "test Demonstrates not stealing of transaction between concurrent threads"() {
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

    def "test Demonstrates connection on explicit transaction"() {
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

    def "test Demonstrates nested transaction"() {
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
            bugService.nestedTransaction()
        then:
            noExceptionThrown()

        cleanup:
            dbSetup.drop()
            applicationContext.close()
    }

    def "test Demonstrates nested transaction 2"() {
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
            bugService.nestedTransaction2()
        then:
            noExceptionThrown()

        cleanup:
            dbSetup.drop()
            applicationContext.close()
    }

    def "test Demonstrates presence of transaction in @Transaction default methods"() {
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
            bugService.noTransactionOrConnectionInDefaultMethod()
        then:
            noExceptionThrown()

        cleanup:
            dbSetup.drop()
            applicationContext.close()
    }

    def "test executor work after commit uses a separate transaction"() {
        given:
            ApplicationContext applicationContext = new DefaultApplicationContext("test")
            applicationContext.environment.addPropertySource(MapPropertySource.of(
                    'test',
                    ['datasources.default': [:]]
            ))
            applicationContext.start()
            DatabaseSetup dbSetup = null

        when:
            dbSetup = applicationContext.getBean(DatabaseSetup)
            def service = applicationContext.getBean(ExecutorTransactionIsolationService)
            dbSetup.initialize()
            def result = service.executeAsyncTransactionAfterCommit()

        then:
            result.count() == 2
            result.innerNewTransaction()

        cleanup:
            if (dbSetup != null) {
                dbSetup.drop()
            }
            applicationContext.close()
    }
}
