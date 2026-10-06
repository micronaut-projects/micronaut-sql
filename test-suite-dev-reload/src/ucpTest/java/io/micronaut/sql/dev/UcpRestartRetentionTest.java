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
package io.micronaut.sql.dev;

import io.micronaut.context.ApplicationContext;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import io.micronaut.jdbc.DataSourceResolver;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.UniversalConnectionPoolLifeCycleState;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs an application with a UCP pool of an H2 database through the development runtime, which restarts the
 * application on a change. The pool factory declares its pools retainable, so the next generation is served the same
 * pool, still running in the connection pool manager, with no {@code micronaut.dev.retain} entry. Configuration under
 * {@code datasources} releases the pool.
 */
class UcpRestartRetentionTest {

    private static final String GREETER = "example.Greeter";

    @TempDir
    Path project;

    @BeforeAll
    static void initializeH2() throws ClassNotFoundException {
        // H2 preallocates an exception as it initializes TraceObject, whose stack trace would otherwise hold the
        // frames of the first generation that opens a connection, and with them its classes
        Class.forName("org.h2.message.TraceObject", true, UcpRestartRetentionTest.class.getClassLoader());
    }

    @Test
    void thePoolIsRetainedAcrossARestart() throws Exception {
        String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";LOCK_TIMEOUT=10000";
        PoolDataSource pool;
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            database(harness, url);
            // the pool keeps a connection open, so that the in-memory database lives as long as the pool
            harness.property("datasources.default.initial-pool-size", "1");
            harness.property("datasources.default.min-pool-size", "1");
            greeter(harness, "one");
            harness.start();
            pool = pool(harness.context());
            execute(pool, "CREATE TABLE RETAINED (ID INT)");

            greeter(harness, "two");
            harness.reload();
            assertEquals(2, harness.generation());

            ReloadTck.assertRetained(harness, pool);
            assertSame(pool, pool(harness.context()));
            assertEquals(List.of("default"), List.of(manager().getConnectionPoolNames()));
            assertEquals(UniversalConnectionPoolLifeCycleState.LIFE_CYCLE_RUNNING, manager().getConnectionPool("default").getLifeCycleState());
            assertTrue(tableExists(pool, "RETAINED"), "the database of the retained pool was kept");
            // ReloadTck.assertRetiredGenerationsCollected is not asserted, here nor when the pool is released: UCP runs
            // JVM-wide threads (UCP Clock, UCP-ScheduledThreadPoolExecutor-Thread-*, UCP-worker-thread-*) that keep the
            // context class loader of the generation that started them, and outlive the pools
        }
        assertEquals(List.of(), List.of(manager().getConnectionPoolNames()), "the pool is destroyed when the last generation stops");
    }

    @Test
    void aDatasourceConfigurationChangeReleasesThePool() throws Exception {
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            String url = "jdbc:h2:mem:" + UUID.randomUUID();
            database(harness, url);
            greeter(harness, "one");
            harness.start();
            PoolDataSource first = pool(harness.context());
            execute(first, "SELECT 1");

            // the application properties change under datasources, together with a class, so the application restarts
            harness.resource("application.properties", """
                datasources.default.url=%s
                datasources.default.username=sa
                datasources.default.password=
                datasources.default.driver-class-name=org.h2.Driver
                datasources.default.max-pool-size=3
                """.formatted(url));
            greeter(harness, "two");
            harness.reload();
            assertEquals(2, harness.generation());

            PoolDataSource second = pool(harness.context());
            assertNotSame(first, second);
            assertEquals(3, second.getMaxPoolSize());
            // the released pool was destroyed, so the manager runs only the new pool of the same name
            assertEquals(List.of("default"), List.of(manager().getConnectionPoolNames()));
            execute(second, "SELECT 1");
        }
    }

    private static UniversalConnectionPoolManager manager() throws UniversalConnectionPoolException {
        return UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
    }

    private static void database(ReloadHarness harness, String url) {
        harness.property("datasources.default.url", url);
        harness.property("datasources.default.username", "sa");
        harness.property("datasources.default.password", "");
        harness.property("datasources.default.driver-class-name", "org.h2.Driver");
    }

    private static void greeter(ReloadHarness harness, String greeting) {
        harness.source(GREETER, """
            package example;

            @jakarta.inject.Singleton
            public class Greeter {
                private final javax.sql.DataSource dataSource;

                public Greeter(javax.sql.DataSource dataSource) {
                    this.dataSource = dataSource;
                }

                public String greet() {
                    return "%s";
                }
            }
            """.formatted(greeting));
    }

    private static PoolDataSource pool(ApplicationContext context) {
        DataSourceResolver resolver = context.findBean(DataSourceResolver.class).orElse(DataSourceResolver.DEFAULT);
        return (PoolDataSource) resolver.resolve(context.getBean(DataSource.class));
    }

    private static void execute(DataSource dataSource, String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private static boolean tableExists(DataSource dataSource, String table) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             ResultSet tables = connection.getMetaData().getTables(null, null, table, null)) {
            return tables.next();
        }
    }
}
