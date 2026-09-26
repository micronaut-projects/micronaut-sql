package io.micronaut.docs.jdbc.transactions;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(transactional = false)
class BookRepositoryTest {

    @Inject
    BookRepository bookRepository;

    @Test
    void testSaveBookInTransaction() throws SQLException {
        bookRepository.createTable();
        int before = bookRepository.count();
        bookRepository.saveBook(new Book("The Stand", 1000));
        assertEquals(before + 1, bookRepository.count());
    }
}
