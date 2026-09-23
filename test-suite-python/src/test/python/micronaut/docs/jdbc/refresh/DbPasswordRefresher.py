# tag::imports[]
from typing import Annotated

from jakarta.inject import Singleton
from micronaut.context import ApplicationContext
from micronaut.context.annotation import Value
from micronaut.runtime.context.scope.refresh import RefreshEvent
from micronaut.scheduling.annotation import Scheduled

from micronaut.docs.jdbc.refresh.DbSecretStore import DbSecretStore
# end::imports[]
from micronaut.context.annotation import Requires


@Requires(property="spec.name", value="DbPasswordRefresherTest")
# tag::clazz[]
@Singleton
class DbPasswordRefresher:
    def __init__(self,
                 current_password: Annotated[str, Value("${datasources.default.password}")],
                 application_context: ApplicationContext,
                 secret_store: DbSecretStore):
        self.current_password = current_password
        self.application_context = application_context
        self.secret_store = secret_store

    @Scheduled(cron="0 * * * * *")  # Runs every minute
    def refresh(self) -> None:
        password = self.get_secret_db_password()  # Read from Vault, Secret Service, etc.
        if password and password != self.current_password:
            # This refresh() call is required before publishing event since datasources.default.password
            # needs to be refreshed in the application configuration
            self.application_context.getEnvironment().refresh()
            # publishEvent with such RefreshEvent will trigger connection pool update and old connections eviction
            # The datasource event handler for this event will get actual password from the
            # application configuration that has been refreshed in refresh() call above
            # and sending such event without prior calling refresh() will not work properly
            self.application_context.publishEvent(RefreshEvent({"datasources.default.password": password}))
            self.current_password = password

    def get_secret_db_password(self) -> str | None:
        return self.secret_store.current_password()
# end::clazz[]
