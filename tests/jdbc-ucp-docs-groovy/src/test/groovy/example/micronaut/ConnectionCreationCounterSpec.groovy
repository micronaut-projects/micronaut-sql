package example.micronaut

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import javax.sql.DataSource

@MicronautTest(transactional = false)
class ConnectionCreationCounterSpec extends Specification {

    @Inject
    DataSource dataSource

    @Inject
    ConnectionCreationCounter counter

    void "the customizer sees created connections"() {
        when:
        dataSource.connection.withCloseable { assert it.isValid(1) }

        then:
        counter.created > 0
    }
}
