package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.url", value = "jdbc:h2:mem:default;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.H2Dialect")
@Property(name = "jpa.default.compile-time-hibernate-proxies", value = "true")
public class H2App extends AbstractApp {
}
