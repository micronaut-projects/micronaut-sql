# tag::imports[]
from java.util.concurrent.atomic import AtomicInteger

from micronaut.configuration.jdbc.ucp import PoolDataSourceCustomizer
from jakarta.inject import Singleton
from oracle.ucp import ConnectionCreationInformation
from oracle.ucp.jdbc import PoolDataSource
# end::imports[]


# tag::clazz[]
@Singleton
class ConnectionCreationCounter(PoolDataSourceCustomizer):
    def __init__(self) -> None:
        self._created = AtomicInteger()

    def customize(self, data_source_name: str, pool: PoolDataSource) -> None:  # <1>
        if data_source_name == "default":
            pool.registerConnectionCreationConsumer(self._on_created)  # <2>

    def _on_created(self, info: ConnectionCreationInformation) -> None:
        if info.getStatus() == ConnectionCreationInformation.Status.SUCCESS:
            self._created.incrementAndGet()

    def created(self) -> int:
        return self._created.get()
# end::clazz[]
