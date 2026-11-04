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
class ConnectionCreationCounter implements PoolDataSourceCustomizer {

    private final AtomicInteger createdCount = new AtomicInteger()

    @Override
    void customize(String dataSourceName, PoolDataSource pool) { // <1>
        if (dataSourceName == "default") {
            pool.registerConnectionCreationConsumer { ConnectionCreationInformation info -> // <2>
                if (info.status == ConnectionCreationInformation.Status.SUCCESS) {
                    createdCount.incrementAndGet()
                }
            }
        }
    }

    int getCreated() {
        createdCount.get()
    }
}
// end::clazz[]
