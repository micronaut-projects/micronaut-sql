/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.jdbc.dbcp.sync;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import io.micronaut.data.connection.ConnectionOperations;
import io.micronaut.data.connection.jdbc.advice.DelegatingDataSource;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.runtime.context.scope.refresh.RefreshEvent;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Credentials refresh of a DBCP datasource wrapped by Micronaut Data, as in an application using Micronaut Data JDBC.
 */
class CredentialsRefreshTest {

    private static final String PASSWORD_PROPERTY = "dbcp-credentials-refresh-test-password";

    @AfterEach
    void clearPassword() {
        System.clearProperty(PASSWORD_PROPERTY);
    }

    @Test
    void passwordChangeIsAppliedToDataSourceWrappedByMicronautData() {
        System.setProperty(PASSWORD_PROPERTY, "");
        try (ApplicationContext context = ApplicationContext.run(Map.of(
            "datasources.default.url", "jdbc:h2:mem:dbcpCredentialsRefresh;DB_CLOSE_DELAY=-1",
            "datasources.default.driver-class-name", "org.h2.Driver",
            "datasources.default.username", "sa",
            "datasources.default.password", "${" + PASSWORD_PROPERTY + "}"
        ))) {
            DataSource dataSource = context.getBean(DataSource.class);
            assertInstanceOf(DelegatingDataSource.class, dataSource, "the datasource is wrapped by Micronaut Data");
            @SuppressWarnings("unchecked")
            ConnectionOperations<Connection> connectionOperations = context.getBean(Argument.of(ConnectionOperations.class, Connection.class));

            execute(connectionOperations, "ALTER USER SA SET PASSWORD 'new_pwd'");
            System.setProperty(PASSWORD_PROPERTY, "new_pwd");
            context.publishEvent(new RefreshEvent(context.getEnvironment().refreshAndDiff()));

            BasicDataSource pool = (BasicDataSource) context.getBean(DataSourceResolver.class).resolve(dataSource);
            assertEquals("new_pwd", pool.getPassword());
            // the pool was restarted, so this connection authenticates with the new password
            assertEquals(1, selectOne(connectionOperations));
        }
    }

    private static void execute(ConnectionOperations<Connection> connectionOperations, String sql) {
        connectionOperations.executeWrite(status -> {
            try (Statement statement = status.getConnection().createStatement()) {
                statement.execute(sql);
                return null;
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static int selectOne(ConnectionOperations<Connection> connectionOperations) {
        return connectionOperations.executeRead(status -> {
            try (Statement statement = status.getConnection().createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT 1")) {
                resultSet.next();
                return resultSet.getInt(1);
            } catch (SQLException e) {
                throw new IllegalStateException(e);
            }
        });
    }
}
