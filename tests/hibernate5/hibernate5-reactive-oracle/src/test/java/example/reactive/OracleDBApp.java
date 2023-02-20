package example.reactive;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest(transactional = false)
@Property(name = "jpa.default.properties.hibernate.connection.db-type", value = "oracle")
@Property(name = "jpa.default.reactive", value = "true")
@Property(name = "test-resources.containers.oracle.image-name", value = "gvenzl/oracle-xe:slim")
public class OracleDBApp extends AbstractApp {
}
