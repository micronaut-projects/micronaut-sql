package io.micronaut.docs.jdbc.refreshdiff

// tag::imports[]
import io.micronaut.context.ApplicationContext
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
// end::imports[]
import io.micronaut.context.annotation.Requires

@Requires(property = "spec.name", value = "DbPasswordDiffRefresherTest")
// tag::clazz[]
@Singleton
open class DbPasswordRefresher(private val applicationContext: ApplicationContext) {

    @Scheduled(cron = "0 * * * * *")
    open fun refresh() {
        // refreshAndDiff() reloads config values (Vault, Secret, etc.), updates the application configuration
        // and returns the changed keys. When ${DB_PASSWORD} changes, the properties that reference it,
        // such as datasources.default.password, are reported as changed too.
        val changes = applicationContext.environment.refreshAndDiff()
        if (changes.isNotEmpty()) {
            // The datasource event handler for this event will get actual password from the
            // application configuration that has been refreshed in refreshAndDiff() call above
            applicationContext.publishEvent(RefreshEvent(changes))
        }
    }
}
// end::clazz[]
