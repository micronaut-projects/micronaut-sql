# tag::imports[]
from typing import Annotated

from jakarta.inject import Inject, Named, Singleton
from javax.sql import DataSource
from micronaut.transaction.annotation import Transactional
# end::imports[]


# tag::clazz[]
@Singleton
class InventoryService:
    data_source: Annotated[DataSource, Inject]  # <1>
    warehouse_data_source: Annotated[DataSource, Inject, Named("warehouse")]  # <2>

    @Transactional  # <3>
    def default_url(self) -> str:
        return self._url(self.data_source)

    @Transactional("warehouse")  # <4>
    def warehouse_url(self) -> str:
        return self._url(self.warehouse_data_source)

    @staticmethod
    def _url(data_source: DataSource) -> str:
        connection = data_source.getConnection()
        try:
            return connection.getMetaData().getURL()
        finally:
            connection.close()
# end::clazz[]
