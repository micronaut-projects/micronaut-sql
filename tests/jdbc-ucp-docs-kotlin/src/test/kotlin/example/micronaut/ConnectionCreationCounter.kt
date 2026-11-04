package example.micronaut

// tag::imports[]
import io.micronaut.configuration.jdbc.ucp.PoolDataSourceCustomizer
import jakarta.inject.Singleton
import oracle.ucp.ConnectionCreationInformation
import oracle.ucp.jdbc.PoolDataSource
import java.util.concurrent.atomic.AtomicInteger
// end::imports[]

// tag::clazz[]
@Singleton
class ConnectionCreationCounter : PoolDataSourceCustomizer {

    private val created = AtomicInteger()

    override fun customize(dataSourceName: String, pool: PoolDataSource) { // <1>
        if (dataSourceName == "default") {
            pool.registerConnectionCreationConsumer { info -> // <2>
                if (info.status == ConnectionCreationInformation.Status.SUCCESS) {
                    created.incrementAndGet()
                }
            }
        }
    }

    fun created(): Int = created.get()
}
// end::clazz[]
