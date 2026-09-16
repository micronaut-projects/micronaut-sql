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
        // refreshAndDiff() will register if there were changes in config values (Vault, Secret, etc.),
        // update application configuration and populate the changes map.
        val changes = applicationContext.environment.refreshAndDiff()
        // ${DB_PASSWORD} placeholder in refreshAndDiff() call will be populated as `db-password` key in changes map
        if (changes.containsKey("db-password")) {
            val password = changes["db-password"] as String
            // The datasource event handler for this event will get actual password from the
            // application configuration that has been refreshed in refreshAndDiff() call above
            applicationContext.publishEvent(RefreshEvent(mapOf<String, Any>("datasources.default.password" to password)))
        }
    }
}
// end::clazz[]
