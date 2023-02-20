package example.jooq.sync;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "postgres")
@Property(name = "jooq.datasources.default.sql-dialect", value = "postgres")
@Property(name = "test-resources.containers.postgres.image-name", value = "postgres:10")
public class PostgresApp extends AbstractApp {
}

