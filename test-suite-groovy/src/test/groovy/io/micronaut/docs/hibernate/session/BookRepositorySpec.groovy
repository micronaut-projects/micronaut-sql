package io.micronaut.docs.hibernate.session

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(transactional = false)
class BookRepositorySpec extends Specification {

    @Inject
    BookRepository bookRepository

    void "test injected entity managers"() {
        when:
        Book book = bookRepository.save("The Stand")

        then:
        book.id != null
        bookRepository.findById(book.id).title == "The Stand"
        bookRepository.findInOtherById(book.id) == null

        when:
        Book other = bookRepository.saveToOther("It")

        then:
        other.id != null
        bookRepository.findInOtherById(other.id).title == "It"
    }
}
