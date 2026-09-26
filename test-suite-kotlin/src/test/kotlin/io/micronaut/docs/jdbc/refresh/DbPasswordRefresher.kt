package io.micronaut.docs.jdbc.refresh

// tag::imports[]
import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Value
import io.micronaut.runtime.context.scope.refresh.RefreshEvent
import io.micronaut.scheduling.annotation.Scheduled
import jakarta.inject.Singleton
// end::imports[]
import io.micronaut.context.annotation.Requires

@Requires(property = "spec.name", value = "DbPasswordRefresherTest")
// tag::clazz[]
@Singleton
open class DbPasswordRefresher(
    @Value("\${datasources.default.password}") private var currentPassword: String,
    private val applicationContext: ApplicationContext,
    private val secretStore: DbSecretStore
) {

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    open fun refresh() {
        val password = getSecretDbPassword() // Read from Vault, Secret Service, etc.
        if (!password.isNullOrEmpty() && password != currentPassword) {
            // This refresh() call is required before publishing event since datasources.default.password
            // needs to be refreshed in the application configuration
            applicationContext.environment.refresh()
            // publishEvent with such RefreshEvent will trigger connection pool update and old connections eviction
            // The datasource event handler for this event will get actual password from the
            // application configuration that has been refreshed in refresh() call above
            // and sending such event without prior calling refresh() will not work properly
            applicationContext.publishEvent(RefreshEvent(mapOf<String, Any>("datasources.default.password" to password)))
            currentPassword = password
        }
    }

    private fun getSecretDbPassword(): String? = secretStore.currentPassword()
}
// end::clazz[]
