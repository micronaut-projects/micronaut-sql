from typing import Annotated

from jakarta.inject import Inject
from javax.sql import DataSource
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.micronaut.ConnectionCreationCounter import ConnectionCreationCounter


@MicronautTest(transactional=False)
class ConnectionCreationCounterTest:
    data_source: Annotated[DataSource, Inject]
    counter: Annotated[ConnectionCreationCounter, Inject]

    @Test
    def test_customizer_sees_created_connections(self):
        connection = self.data_source.getConnection()
        try:
            assert connection.isValid(1)
        finally:
            connection.close()
        assert self.counter.created() > 0
