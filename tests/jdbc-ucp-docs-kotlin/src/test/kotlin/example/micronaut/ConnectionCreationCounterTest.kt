package example.micronaut

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import javax.sql.DataSource

@MicronautTest(transactional = false)
class ConnectionCreationCounterTest {

    @Inject
    lateinit var dataSource: DataSource

    @Inject
    lateinit var counter: ConnectionCreationCounter

    @Test
    fun customizerSeesCreatedConnections() {
        dataSource.connection.use { assertTrue(it.isValid(1)) }
        assertTrue(counter.created() > 0)
    }
}
