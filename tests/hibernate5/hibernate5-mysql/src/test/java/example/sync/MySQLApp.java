package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.dialect", value = "mysql")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.MySQL8Dialect")
@Property(name = "jpa.default.compile-time-hibernate-proxies", value = "true")
public class MySQLApp extends AbstractApp {
}
