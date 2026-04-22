package example.jdbc.ucp.sqlite;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.TestInstance;

@MicronautTest(transactional = false)
@Property(name = "datasources.default.db-type", value = "sqlite")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLiteApp extends AbstractApp {
}
