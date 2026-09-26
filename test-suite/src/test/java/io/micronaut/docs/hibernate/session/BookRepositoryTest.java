package io.micronaut.docs.hibernate.session;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@MicronautTest(transactional = false)
class BookRepositoryTest {

    @Inject
    BookRepository bookRepository;

    @Test
    void testInjectedEntityManagers() {
        Book book = bookRepository.save("The Stand");
        assertNotNull(book.getId());
        assertEquals("The Stand", bookRepository.findById(book.getId()).getTitle());
        assertNull(bookRepository.findInOtherById(book.getId()));

        Book other = bookRepository.saveToOther("It");
        assertNotNull(other.getId());
        assertEquals("It", bookRepository.findInOtherById(other.getId()).getTitle());
    }
}
