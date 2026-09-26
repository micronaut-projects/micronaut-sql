package io.micronaut.jdbc

import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import spock.lang.AutoCleanup
import spock.lang.Specification

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

    void "full refresh ignores a username changed to empty"() {
        when:
        System.setProperty(USERNAME, "")
        System.setProperty(PASSWORD, "new-pwd")
        applicationContext.environment.refresh()
        factory.onApplicationEvent(new RefreshEvent())

        then:
        noExceptionThrown()
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

        RecordingFactory(ApplicationContext applicationContext) {
            super(applicationContext)
        }

        @Override
        protected void dataSourceCredentialsChanged(String dataSourceName, DataSourceCredentials dataSourceCredentials) {
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
