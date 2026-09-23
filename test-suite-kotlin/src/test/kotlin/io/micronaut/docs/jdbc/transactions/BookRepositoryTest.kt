package io.micronaut.docs.jdbc.transactions

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class BookRepositoryTest {

    @Inject
    lateinit var bookRepository: BookRepository

    @Test
    fun testSaveBookInTransaction() {
        bookRepository.createTable()
        val before = bookRepository.count()
        bookRepository.saveBook(Book("The Stand", 1000))
        assertEquals(before + 1, bookRepository.count())
    }
}
