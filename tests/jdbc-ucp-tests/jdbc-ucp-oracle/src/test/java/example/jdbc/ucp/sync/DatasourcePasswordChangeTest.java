package example.jdbc.ucp.sync;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import oracle.ucp.jdbc.ValidConnection;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "oracle")
@Property(name = "datasources.default.initial-pool-size", value = "1")
@Property(name = "datasources.default.connection-factory-class-name", value = "oracle.jdbc.pool.OracleDataSource")
@Property(name = "oracle.ucp.destroyOnReload", value = "true")
class DatasourcePasswordChangeTest {

    @Inject
    DataSource dataSource;

    @Inject
    DataSourceResolver dataSourceResolver;

    @Inject
    ApplicationContext applicationContext;

    @Test
    void testStartupAndChangePassword() throws Exception {
        assertNotNull(dataSource);
        ResultSet rs = dataSource.getConnection().prepareStatement("SELECT 1").executeQuery();
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));

        String newPassword = "updated_pwd";
        dataSource.getConnection().prepareStatement(String.format("ALTER USER test IDENTIFIED BY %s", newPassword)).executeUpdate();
        applicationContext.publishEvent(new RefreshEvent(Map.of("datasources.default.password", newPassword)));

        Connection connection = dataSourceResolver.resolve(dataSource).getConnection();
        if (connection instanceof ValidConnection validConnection) {
            // For the test purpose, this is how to invalidate connection
            // and force connection pool to create new one using changed password
            validConnection.setInvalid();
        }

        rs = dataSource.getConnection().prepareStatement("SELECT 1").executeQuery();
        assertTrue(rs.next());
        assertEquals(1, rs.getInt(1));
    }
}
