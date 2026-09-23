package io.micronaut.docs.jdbc.transactions

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(transactional = false)
class BookRepositorySpec extends Specification {

    @Inject
    BookRepository bookRepository

    void "test save book in transaction"() {
        given:
        bookRepository.createTable()
        int before = bookRepository.count()

        when:
        bookRepository.saveBook(new Book("The Stand", 1000))

        then:
        bookRepository.count() == before + 1
    }
}
