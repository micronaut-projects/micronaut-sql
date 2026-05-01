package example.jdbc.hikari.sqlite;

import example.sync.AbstractApp;
import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.sql.SQLException;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
@Property(name = "datasources.default.db-type", value = "sqlite")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SQLiteApp extends AbstractApp {

    @Inject
    PetRepository petRepository;

    @Test
    void shouldEnforceForeignKeys() {
        Pet pet = new Pet(null, "No Owner", null, new Owner(-1L, "Missing", 1));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> petRepository.save(pet));
        SQLException cause = assertInstanceOf(SQLException.class, exception.getCause());
        assertTrue(cause.getMessage().toLowerCase(Locale.ROOT).contains("foreign key"));
    }
}
