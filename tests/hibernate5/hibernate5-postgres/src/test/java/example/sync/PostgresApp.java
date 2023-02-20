package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "postgres")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.PostgreSQL95Dialect")
@Property(name = "jpa.default.compile-time-hibernate-proxies", value = "true")
public class PostgresApp extends AbstractApp {
}
