package io.micronaut.docs.jdbc.multiple

// tag::imports[]
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton

import javax.sql.DataSource
import java.sql.Connection
// end::imports[]

// tag::clazz[]
@Singleton
class InventoryService {

    @Inject
    DataSource dataSource // <1>

    @Inject
    @Named("warehouse")
    DataSource warehouseDataSource // <2>

    @Transactional // <3>
    String defaultUrl() {
        url(dataSource)
    }

    @Transactional("warehouse") // <4>
    String warehouseUrl() {
        url(warehouseDataSource)
    }

    private static String url(DataSource dataSource) {
        try (Connection connection = dataSource.connection) {
            return connection.metaData.URL
        }
    }
}
// end::clazz[]
