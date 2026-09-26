package io.micronaut.docs.jdbc.multiple

// tag::imports[]
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Inject
import jakarta.inject.Named
import jakarta.inject.Singleton
import javax.sql.DataSource
// end::imports[]

// tag::clazz[]
@Singleton
open class InventoryService {

    @Inject
    lateinit var dataSource: DataSource // <1>

    @Inject
    @Named("warehouse")
    lateinit var warehouseDataSource: DataSource // <2>

    @Transactional // <3>
    open fun defaultUrl(): String = url(dataSource)

    @Transactional("warehouse") // <4>
    open fun warehouseUrl(): String = url(warehouseDataSource)

    private fun url(dataSource: DataSource): String =
        dataSource.connection.use { connection -> connection.metaData.url }
}
// end::clazz[]
