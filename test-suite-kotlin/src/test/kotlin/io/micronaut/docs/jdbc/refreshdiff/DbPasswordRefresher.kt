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
            // The values in the changes are the previous values, which may be secrets, and refresh event
            // listeners only need the changed keys, so publish the keys with redacted values.
            // The datasource event handler for this event will get actual password from the
            // application configuration that has been refreshed in refreshAndDiff() call above
            val changedKeys = changes.keys.associateWith<String, Any> { "<redacted>" }
            applicationContext.publishEvent(RefreshEvent(changedKeys))
        }
    }
}
// end::clazz[]
