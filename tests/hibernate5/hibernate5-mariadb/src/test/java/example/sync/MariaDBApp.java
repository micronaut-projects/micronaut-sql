package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.dialect", value = "mariadb")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.MariaDB103Dialect")
@Property(name = "jpa.default.compile-time-hibernate-proxies", value = "true")
public class MariaDBApp extends AbstractApp {
}
