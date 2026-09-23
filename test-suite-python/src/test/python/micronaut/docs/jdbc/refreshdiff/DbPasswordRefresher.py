# tag::imports[]
from jakarta.inject import Singleton
from micronaut.context import ApplicationContext
from micronaut.runtime.context.scope.refresh import RefreshEvent
from micronaut.scheduling.annotation import Scheduled
# end::imports[]
from micronaut.context.annotation import Requires


@Requires(property="spec.name", value="DbPasswordDiffRefresherTest")
# tag::clazz[]
@Singleton
class DbPasswordRefresher:
    def __init__(self, application_context: ApplicationContext):
        self.application_context = application_context

    @Scheduled(cron="0 * * * * *")
    def refresh(self) -> None:
        # refreshAndDiff() will register if there were changes in config values (Vault, Secret, etc.),
        # update application configuration and populate the changes map.
        changes = self.application_context.getEnvironment().refreshAndDiff()
        # ${DB_PASSWORD} placeholder in refreshAndDiff() call will be populated as `db-password` key in changes map
        if changes.containsKey("db-password"):
            password = changes.get("db-password")
            # The datasource event handler for this event will get actual password from the
            # application configuration that has been refreshed in refreshAndDiff() call above
            self.application_context.publishEvent(RefreshEvent({"datasources.default.password": password}))
# end::clazz[]
