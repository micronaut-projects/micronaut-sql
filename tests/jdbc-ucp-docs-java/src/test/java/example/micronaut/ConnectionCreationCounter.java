package example.micronaut;

// tag::imports[]
import io.micronaut.configuration.jdbc.ucp.PoolDataSourceCustomizer;
import jakarta.inject.Singleton;
import oracle.ucp.ConnectionCreationInformation;
import oracle.ucp.jdbc.PoolDataSource;

import java.util.concurrent.atomic.AtomicInteger;
// end::imports[]

// tag::clazz[]
@Singleton
public class ConnectionCreationCounter implements PoolDataSourceCustomizer {

    private final AtomicInteger created = new AtomicInteger();

    @Override
    public void customize(String dataSourceName, PoolDataSource pool) { // <1>
        if ("default".equals(dataSourceName)) {
            pool.registerConnectionCreationConsumer(this::onCreated); // <2>
        }
    }

    private void onCreated(ConnectionCreationInformation info) {
        if (info.getStatus() == ConnectionCreationInformation.Status.SUCCESS) {
            created.incrementAndGet();
        }
    }

    public int getCreated() {
        return created.get();
    }
}
// end::clazz[]
