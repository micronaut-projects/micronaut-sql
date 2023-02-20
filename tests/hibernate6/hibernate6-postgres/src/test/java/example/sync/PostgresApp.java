package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "postgres")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.PostgreSQL95Dialect")
@Property(name = "test-resources.containers.postgres.image-name", value = "postgres:9.6.12")
public class PostgresApp extends AbstractApp {
}
