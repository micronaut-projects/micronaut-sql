package example.sync;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;

@MicronautTest
@Property(name = "datasources.default.db-type", value = "oracle")
@Property(name = "jpa.default.properties.hibernate.dialect", value = "org.hibernate.dialect.Oracle10gDialect")
@Property(name = "test-resources.containers.oracle.image-name", value = "gvenzl/oracle-xe:slim")
public class OracleDBApp extends AbstractApp {
}
