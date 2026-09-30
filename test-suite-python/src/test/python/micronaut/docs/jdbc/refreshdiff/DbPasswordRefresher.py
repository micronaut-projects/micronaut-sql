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
        # refreshAndDiff() reloads config values (Vault, Secret, etc.), updates the application configuration
        # and returns the changed keys. When ${DB_PASSWORD} changes, the properties that reference it,
        # such as datasources.default.password, are reported as changed too.
        changes = self.application_context.getEnvironment().refreshAndDiff()
        if not changes.isEmpty():
            # The values in the changes are the previous values, which may be secrets, and refresh event
            # listeners only need the changed keys, so publish the keys with redacted values.
            # The datasource event handler for this event will get actual password from the
            # application configuration that has been refreshed in refreshAndDiff() call above
            changed_keys = {key: "<redacted>" for key in changes.keySet()}
            self.application_context.publishEvent(RefreshEvent(changed_keys))
# end::clazz[]
