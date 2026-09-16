package io.micronaut.docs.jdbc.refresh;

// tag::imports[]
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Value;
import io.micronaut.core.util.StringUtils;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.util.Map;
// end::imports[]
import io.micronaut.context.annotation.Requires;

@Requires(property = "spec.name", value = "DbPasswordRefresherTest")
// tag::clazz[]
@Singleton
final class DbPasswordRefresher {

    private final ApplicationContext applicationContext;
    private final DbSecretStore secretStore;
    private String currentPassword;

    public DbPasswordRefresher(@Value("${datasources.default.password}") String currentPassword,
                               ApplicationContext applicationContext,
                               DbSecretStore secretStore) {
        this.currentPassword = currentPassword;
        this.applicationContext = applicationContext;
        this.secretStore = secretStore;
    }

    @Scheduled(cron = "0 * * * * *") // Runs every minute
    void refresh() {
        String password = getSecretDbPassword(); // Read from Vault, Secret Service, etc.
        if (StringUtils.isNotEmpty(password) && !password.equals(currentPassword)) {
            // This refresh() call is required before publishing event since datasources.default.password
            // needs to be refreshed in the application configuration
            applicationContext.getEnvironment().refresh();
            // publishEvent with such RefreshEvent will trigger connection pool update and old connections eviction
            // The datasource event handler for this event will get actual password from the
            // application configuration that has been refreshed in refresh() call above
            // and sending such event without prior calling refresh() will not work properly
            applicationContext.publishEvent(new RefreshEvent(Map.of("datasources.default.password", password)));
            this.currentPassword = password;
        }
    }

    private String getSecretDbPassword() {
        return secretStore.currentPassword();
    }
}
// end::clazz[]
