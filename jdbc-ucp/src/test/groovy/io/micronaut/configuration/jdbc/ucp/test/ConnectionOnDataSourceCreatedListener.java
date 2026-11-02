package io.micronaut.configuration.jdbc.ucp.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.util.StringUtils;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * A {@link BeanCreatedEventListener} that takes a connection as soon as a {@link DataSource} is created, the way the
 * Liquibase and Flyway migration runners do. It has the default order, so it runs before the
 * {@code ConnectionPoolManagerListener}. Turned on by setting {@code test.connection-on-data-source-created} to
 * {@code true}.
 */
@Singleton
@Requires(env = "test")
@Requires(property = "test.connection-on-data-source-created", value = StringUtils.TRUE)
public class ConnectionOnDataSourceCreatedListener implements BeanCreatedEventListener<DataSource> {

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        try (Connection connection = event.getBean().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("SELECT 1");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to take a connection from the data source", e);
        }
        return event.getBean();
    }
}
