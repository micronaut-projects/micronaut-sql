package example.reactive;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest(transactional = false)
@Property(name = "jpa.default.properties.hibernate.connection.db-type", value = "mysql")
@Property(name = "jpa.default.reactive", value = "true")
@Property(name = "test-resources.containers.mysql.image-name", value = "mysql:8.0.30")
public class MySQLApp extends AbstractApp {
}
