package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "mysql")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.MySQL8Dialect")
@Property(name = "test-resources.containers.mysql.image-name", value = "mysql:8.0.30")
public class MySQLApp extends AbstractApp {
}
