package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "mariadb")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.MariaDB103Dialect")
@Property(name = "test-resources.containers.mariadb.image-name", value = "mariadb:10.9.3")
public class MariaDBApp extends AbstractApp {
}
