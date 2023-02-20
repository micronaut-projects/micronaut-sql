package example.jooq.reactive;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

import java.time.Duration;

@MicronautTest(transactional = false)
@Property(name = "r2dbc.datasources.default.db-type", value = "postgres")
@Property(name = "jooq.r2dbc-datasources.default.sql-dialect", value = "postgres")
@Property(name = "r2dbc.datasources.default.options.driver", value = "pool")
@Property(name = "r2dbc.datasources.default.options.protocol", value = "postgresql")
@Property(name = "r2dbc.datasources.default.options.connectTimeout", value = "PT1M")
@Property(name = "r2dbc.datasources.default.options.statementTimeout", value = "PT1M")
@Property(name = "r2dbc.datasources.default.options.lockTimeout", value = "PT1M")
@Property(name = "test-resources.containers.postgres.image-name", value = "postgres:10")
public class PostgresApp extends AbstractApp {
}
