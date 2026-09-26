package io.micronaut.docs.jdbc.refreshdiff;

// tag::imports[]
import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Singleton;

import java.util.Map;
// end::imports[]
import io.micronaut.context.annotation.Requires;

@Requires(property = "spec.name", value = "DbPasswordDiffRefresherTest")
// tag::clazz[]
@Singleton
final class DbPasswordRefresher {

    private final ApplicationContext applicationContext;

    public DbPasswordRefresher(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Scheduled(cron = "0 * * * * *")
    void refresh() {
        // refreshAndDiff() will register if there were changes in config values (Vault, Secret, etc.),
        // update application configuration and populate the changes map.
        Map<String, Object> changes = applicationContext.getEnvironment().refreshAndDiff();
        // ${DB_PASSWORD} placeholder in refreshAndDiff() call will be populated as `db-password` key in changes map
        if (changes.containsKey("db-password")) {
            String password = (String) changes.get("db-password");
            // The datasource event handler for this event will get actual password from the
            // application configuration that has been refreshed in refreshAndDiff() call above
            applicationContext.publishEvent(new RefreshEvent(Map.of("datasources.default.password", password)));
        }
    }
}
// end::clazz[]
