package io.micronaut.docs.jdbc.multiple;

// tag::imports[]
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
// end::imports[]

// tag::clazz[]
@Singleton
public class InventoryService {

    @Inject
    DataSource dataSource; // <1>

    @Inject
    @Named("warehouse")
    DataSource warehouseDataSource; // <2>

    @Transactional // <3>
    public String defaultUrl() throws SQLException {
        return url(dataSource);
    }

    @Transactional("warehouse") // <4>
    public String warehouseUrl() throws SQLException {
        return url(warehouseDataSource);
    }

    private static String url(DataSource dataSource) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getURL();
        }
    }
}
// end::clazz[]
