package io.micronaut.jdbc

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import spock.lang.AutoCleanup
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BaseDatasourceFactorySpec extends Specification {

    private static final String USERNAME = "base-ds-factory-spec-username"
    private static final String PASSWORD = "base-ds-factory-spec-password"

    @AutoCleanup
    ApplicationContext applicationContext

    RecordingFactory factory

    void setup() {
        System.setProperty(USERNAME, "sa")
        System.setProperty(PASSWORD, "pwd")
        applicationContext = ApplicationContext.run([
                'datasources.default.username': '${' + USERNAME + '}',
                'datasources.default.password': '${' + PASSWORD + '}',
                'datasources.other.url'       : 'jdbc:h2:mem:other'
        ])
        factory = new RecordingFactory(applicationContext)
    }

    void cleanup() {
        System.clearProperty(USERNAME)
        System.clearProperty(PASSWORD)
    }

    void "known credentials do not keep the plaintext password"() {
        when:
        def field = BaseDatasourceFactory.getDeclaredField("knownCredentials")
        field.accessible = true
        Map<String, Object> knownCredentials = field.get(factory)
        def defaultCredentials = knownCredentials.get("default")

        then:
        defaultCredentials.userName() == "sa"
        defaultCredentials.passwordDigest() != null
        defaultCredentials.passwordDigest() != "pwd"
    }

    void "refresh events are handled one at a time"() {
        given:
        def updateStarted = new CountDownLatch(1)
        def releaseUpdate = new CountDownLatch(1)
        def blockingFactory = new RecordingFactory(applicationContext) {
            @Override
            protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
                updateStarted.countDown()
                releaseUpdate.await(5, TimeUnit.SECONDS)
                super.dataSourceCredentialsChanged(dataSourceName, dataSourceCredentials)
            }
        }
        System.setProperty(PASSWORD, "new-pwd")
        applicationContext.environment.refresh()

        when: "a second event is published while the first one is updating the pool"
        def first = Thread.start { blockingFactory.onApplicationEvent(new RefreshEvent()) }
        updateStarted.await(5, TimeUnit.SECONDS)
        def second = Thread.start { blockingFactory.onApplicationEvent(new RefreshEvent()) }
        second.join(300)

        then: "the second event waits for the first one"
        second.alive

        when:
        releaseUpdate.countDown()
        first.join(5000)
        second.join(5000)

        then: "and does not apply the same change again"
        blockingFactory.changes == [new Change("default", null, "new-pwd")]
    }

    void "only new RefreshEvent() is a full refresh, like in the refresh scope"() {
        given:
        System.setProperty(PASSWORD, "new-pwd")
        applicationContext.environment.refresh()

        when: "an event with an equal but different source map is published"
        factory.onApplicationEvent(new RefreshEvent(["all": "*"]))

        then: "it is handled as a refresh of the 'all' key"
        factory.changes.isEmpty()

        when:
        factory.onApplicationEvent(new RefreshEvent())

        then:
        factory.changes == [new Change("default", null, "new-pwd")]
    }

    void "full refresh without credential changes does not notify"() {
        when:
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then:
        factory.changes.isEmpty()
    }

    void "full refresh notifies only the changed password"() {
        when:
        System.setProperty(PASSWORD, "new-pwd")
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then:
        factory.changes == [new Change("default", null, "new-pwd")]

        when: "the same full refresh is published again"
        factory.changes.clear()
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then: "nothing changed since the last refresh"
        factory.changes.isEmpty()
    }

    void "full refresh before the environment is refreshed does not notify, the next one does"() {
        when: "the secret changes but the environment is not refreshed"
        System.setProperty(PASSWORD, "new-pwd")
        factory.onApplicationEvent(new RefreshEvent())

        then: "the environment still holds the old value, so nothing changed"
        applicationContext.getProperty('datasources.default.password', String).get() == "pwd"
        factory.changes.isEmpty()

        when: "the environment is refreshed and the event is published again"
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then: "the change is not lost"
        factory.changes == [new Change("default", null, "new-pwd")]
    }

    void "full refresh notifies username and password changes together"() {
        when:
        System.setProperty(USERNAME, "admin")
        System.setProperty(PASSWORD, "new-pwd")
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then:
        factory.changes == [new Change("default", "admin", "new-pwd")]
    }

    void "full refresh ignores the whole change when the username is changed to empty and applies it once fixed"() {
        when:
        System.setProperty(USERNAME, "")
        System.setProperty(PASSWORD, "new-pwd")
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then: "the new password is not combined with the old username"
        noExceptionThrown()
        factory.changes.isEmpty()

        when: "the username is fixed"
        System.setProperty(USERNAME, "sa")
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then: "the pending password change is applied"
        factory.changes == [new Change("default", null, "new-pwd")]
    }

    void "full refresh ignores the whole change when the password can no longer be read"() {
        when:
        System.setProperty(USERNAME, "admin")
        System.clearProperty(PASSWORD)
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then: "the new username is not combined with the old password"
        noExceptionThrown()
        factory.changes.isEmpty()
    }

    void "failed update is retried on the next full refresh"() {
        given:
        factory.failingDataSource = "default"
        System.setProperty(PASSWORD, "new-pwd")
        applicationContext.environment.refresh()

        when:
        factory.onApplicationEvent(new RefreshEvent())

        then:
        noExceptionThrown()
        factory.changes.isEmpty()

        when:
        factory.failingDataSource = null
        factory.onApplicationEvent(new RefreshEvent())

        then:
        factory.changes == [new Change("default", null, "new-pwd")]
    }

    void "full refresh after a targeted refresh does not notify the same change twice"() {
        when:
        System.setProperty(PASSWORD, "new-pwd")
        def diff = applicationContext.environment.refreshAndDiff()
        factory.onApplicationEvent(new RefreshEvent(diff))

        then:
        factory.changes == [new Change("default", null, "new-pwd")]

        when:
        factory.changes.clear()
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then:
        factory.changes.isEmpty()
    }

    void "targeted refresh ignores the whole change when the username is changed to empty"() {
        when:
        System.setProperty(USERNAME, "")
        System.setProperty(PASSWORD, "new-pwd")
        factory.onApplicationEvent(new RefreshEvent(applicationContext.environment.refreshAndDiff()))

        then: "the new password is not combined with the old username"
        noExceptionThrown()
        factory.changes.isEmpty()
    }

    void "targeted refresh ignores a password that can no longer be read"() {
        when:
        System.clearProperty(PASSWORD)
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent(['datasources.default.password': 'old-value']))

        then:
        noExceptionThrown()
        factory.changes.isEmpty()
    }

    void "targeted refresh ignores the whole change when the password can no longer be read"() {
        when:
        System.setProperty(USERNAME, "admin")
        System.clearProperty(PASSWORD)
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent([
                'datasources.default.username': 'old-value',
                'datasources.default.password': 'old-value'
        ]))

        then: "the new username is not combined with the old password"
        noExceptionThrown()
        factory.changes.isEmpty()
    }

    void "targeted refresh applies a change skipped earlier together with the fixed username"() {
        when: "the username is changed to empty together with a new password"
        System.setProperty(USERNAME, "")
        System.setProperty(PASSWORD, "new-pwd")
        factory.onApplicationEvent(new RefreshEvent(applicationContext.environment.refreshAndDiff()))

        then:
        factory.changes.isEmpty()

        when: "a later event reports only the fixed username"
        System.setProperty(USERNAME, "admin")
        factory.onApplicationEvent(new RefreshEvent(applicationContext.environment.refreshAndDiff()))

        then: "the pending password is applied together with the username"
        factory.changes == [new Change("default", "admin", "new-pwd")]

        when:
        factory.changes.clear()
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then: "nothing is pending anymore"
        factory.changes.isEmpty()
    }

    void "nested username and password properties are not datasource credentials"() {
        given:
        System.setProperty("base-ds-factory-spec-nested-password", "pwd")
        def context = ApplicationContext.run([
                'datasources.nested.url'                                 : 'jdbc:h2:mem:nested',
                'datasources.nested.data-source-properties.password'     : '${base-ds-factory-spec-nested-password}'
        ])
        def recordingFactory = new RecordingFactory(context)

        when:
        System.setProperty("base-ds-factory-spec-nested-password", "new-pwd")
        def changes = context.environment.refreshAndDiff()
        recordingFactory.onApplicationEvent(new RefreshEvent(changes))

        then:
        changes.containsKey('datasources.nested.data-source-properties.password')
        recordingFactory.changes.isEmpty()

        cleanup:
        System.clearProperty("base-ds-factory-spec-nested-password")
        context.close()
    }

    void "credentials toString does not expose the password"() {
        expect:
        new BaseDatasourceFactory.DataSourceCredentials("sa", "secret-pwd").toString() == "DataSourceCredentials[userName=sa, password=*****]"
        new BaseDatasourceFactory.DataSourceCredentials("sa", null).toString() == "DataSourceCredentials[userName=sa, password=null]"
    }

    void "targeted refresh without a configuration change does not notify"() {
        when:
        factory.onApplicationEvent(new RefreshEvent(['datasources.default.password': '<redacted>']))

        then:
        factory.changes.isEmpty()
    }

    void "invalid change of one datasource does not prevent other datasources from being updated"() {
        given:
        def context = multipleDataSourcesContext()
        def recordingFactory = new RecordingFactory(context)

        when:
        System.setProperty("base-ds-factory-spec-first-username", "")
        System.setProperty("base-ds-factory-spec-first-password", "new-pwd")
        System.setProperty("base-ds-factory-spec-second-password", "new-pwd")
        recordingFactory.onApplicationEvent(new RefreshEvent(context.environment.refreshAndDiff()))

        then:
        noExceptionThrown()
        recordingFactory.changes == [new Change("second", null, "new-pwd")]

        cleanup:
        clearMultipleDataSourcesProperties()
        context.close()
    }

    void "failure to update one datasource does not fail the event or the other datasources, and is retried"() {
        given:
        def context = multipleDataSourcesContext()
        def recordingFactory = new RecordingFactory(context, "first")

        when:
        System.setProperty("base-ds-factory-spec-first-password", "new-pwd")
        System.setProperty("base-ds-factory-spec-second-password", "new-pwd")
        def changes = context.environment.refreshAndDiff()
        recordingFactory.onApplicationEvent(new RefreshEvent(changes))

        then:
        noExceptionThrown()
        recordingFactory.changes == [new Change("second", null, "new-pwd")]

        when: "the same keys are published again after the failure is gone"
        recordingFactory.failingDataSource = null
        recordingFactory.changes.clear()
        recordingFactory.onApplicationEvent(new RefreshEvent(changes))

        then: "only the failed update is retried"
        recordingFactory.changes == [new Change("first", null, "new-pwd")]

        cleanup:
        clearMultipleDataSourcesProperties()
        context.close()
    }

    private static ApplicationContext multipleDataSourcesContext() {
        System.setProperty("base-ds-factory-spec-first-username", "sa")
        System.setProperty("base-ds-factory-spec-first-password", "pwd")
        System.setProperty("base-ds-factory-spec-second-password", "pwd")
        ApplicationContext.run([
                'datasources.first.username' : '${base-ds-factory-spec-first-username}',
                'datasources.first.password' : '${base-ds-factory-spec-first-password}',
                'datasources.second.password': '${base-ds-factory-spec-second-password}'
        ])
    }

    private static void clearMultipleDataSourcesProperties() {
        System.clearProperty("base-ds-factory-spec-first-username")
        System.clearProperty("base-ds-factory-spec-first-password")
        System.clearProperty("base-ds-factory-spec-second-password")
    }

    void "credentials of a disabled datasource are not refreshed"() {
        given:
        System.setProperty("base-ds-factory-spec-disabled-password", "pwd")
        def context = ApplicationContext.run([
                'datasources.disabled.enabled' : false,
                'datasources.disabled.username': 'sa',
                'datasources.disabled.password': '${base-ds-factory-spec-disabled-password}'
        ])
        def recordingFactory = new RecordingFactory(context)

        when:
        System.setProperty("base-ds-factory-spec-disabled-password", "new-pwd")
        def changes = context.environment.refreshAndDiff()
        recordingFactory.onApplicationEvent(new RefreshEvent(changes))
        recordingFactory.onApplicationEvent(new RefreshEvent())

        then:
        changes.containsKey('datasources.disabled.password')
        recordingFactory.changes.isEmpty()

        cleanup:
        System.clearProperty("base-ds-factory-spec-disabled-password")
        context.close()
    }

    void "unresolvable placeholder in credentials does not fail the factory"() {
        given:
        def context = ApplicationContext.run([
                'datasources.disabled.enabled' : false,
                'datasources.disabled.password': '${base-ds-factory-spec-missing}'
        ])

        when:
        def recordingFactory = new RecordingFactory(context)
        context.environment.refresh()
        recordingFactory.onApplicationEvent(new RefreshEvent())

        then:
        noExceptionThrown()
        recordingFactory.changes.isEmpty()

        cleanup:
        context.close()
    }

    static class RecordingFactory extends BaseDatasourceFactory {

        final List<Change> changes = []
        String failingDataSource

        RecordingFactory(ApplicationContext applicationContext, String failingDataSource = null) {
            super(applicationContext)
            this.failingDataSource = failingDataSource
        }

        @Override
        protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
            if (dataSourceName == failingDataSource) {
                throw new IllegalStateException("Simulated failure for " + dataSourceName)
            }
            changes << new Change(dataSourceName, dataSourceCredentials.userName(), dataSourceCredentials.password())
        }
    }

    @groovy.transform.EqualsAndHashCode
    @groovy.transform.ToString
    static class Change {
        final String name
        final String userName
        final String password

        Change(String name, String userName, String password) {
            this.name = name
            this.userName = userName
            this.password = password
        }
    }
}
