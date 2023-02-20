package example.reactive;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest(transactional = false)
@Property(name = "jpa.default.properties.hibernate.connection.db-type", value = "postgres")
@Property(name = "jpa.default.reactive", value = "true")
@Property(name = "test-resources.containers.postgres.image-name", value = "postgres:10")
public class PostgresApp extends AbstractApp {
}
