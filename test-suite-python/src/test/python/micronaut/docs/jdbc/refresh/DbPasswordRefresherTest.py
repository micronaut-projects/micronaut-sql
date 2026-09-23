from typing import Annotated

from jakarta.inject import Inject
from java.lang import System
from javax.sql import DataSource
from micronaut.context.annotation import Property
from micronaut.jdbc import DataSourceResolver
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import AfterEach, Test

from micronaut.docs.jdbc.refresh.DbPasswordRefresher import DbPasswordRefresher
from micronaut.docs.jdbc.refresh.DbSecretStore import DbSecretStore


@Property(name="spec.name", value="DbPasswordRefresherTest")
@MicronautTest(transactional=False)
class DbPasswordRefresherTest:
    refresher: Annotated[DbPasswordRefresher, Inject]
    secret_store: Annotated[DbSecretStore, Inject]
    data_source: Annotated[DataSource, Inject]
    data_source_resolver: Annotated[DataSourceResolver, Inject]

    @Test
    def test_password_rotation(self):
        assert self.hikari().getPassword() == "initial"

        # the secret service rotates the password, the configuration ("${db-password}") now resolves to the new value
        self.alter_password("rotated")
        System.setProperty("db-password", "rotated")
        self.secret_store.rotate("rotated")

        self.refresher.refresh()

        assert self.hikari().getPassword() == "rotated"
        connection = self.hikari().getConnection()
        try:
            assert connection.getMetaData().getUserName() == "SA"
        finally:
            connection.close()

    @AfterEach
    def restore_password(self):
        self.alter_password("initial")
        System.clearProperty("db-password")
        self.secret_store.rotate("initial")
        self.refresher.refresh()

    def hikari(self):
        return self.data_source_resolver.resolve(self.data_source)  # unwrap the transaction-aware DataSource

    def alter_password(self, password: str):
        connection = self.hikari().getConnection()
        try:
            statement = connection.createStatement()
            statement.execute("ALTER USER sa SET PASSWORD '" + password + "'")
            statement.close()
        finally:
            connection.close()
