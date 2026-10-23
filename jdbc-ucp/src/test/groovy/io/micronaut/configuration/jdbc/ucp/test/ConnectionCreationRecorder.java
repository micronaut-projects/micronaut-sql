package io.micronaut.configuration.jdbc.ucp.test;

import io.micronaut.configuration.jdbc.ucp.PoolDataSourceCustomizer;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;
import oracle.ucp.ConnectionCreationInformation;
import oracle.ucp.jdbc.PoolDataSource;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registers a connection creation consumer, a UCP setting that configuration cannot supply.
 */
@Singleton
@Requires(property = "spec.name", value = "PoolDataSourceCustomizationSpec")
public class ConnectionCreationRecorder implements PoolDataSourceCustomizer {

    private final List<String> customizedDataSources = new CopyOnWriteArrayList<>();
    private final List<ConnectionCreationInformation> createdConnections = new CopyOnWriteArrayList<>();

    @Override
    public void customize(String dataSourceName, PoolDataSource poolDataSource) {
        customizedDataSources.add(dataSourceName);
        poolDataSource.registerConnectionCreationConsumer(createdConnections::add);
    }

    public List<String> getCustomizedDataSources() {
        return customizedDataSources;
    }

    public List<ConnectionCreationInformation> getCreatedConnections() {
        return createdConnections;
    }
}
