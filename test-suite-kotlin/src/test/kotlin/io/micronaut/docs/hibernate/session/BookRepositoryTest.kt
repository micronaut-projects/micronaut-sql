package io.micronaut.docs.hibernate.session

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class BookRepositoryTest {

    @Inject
    lateinit var bookRepository: BookRepository

    @Test
    fun testInjectedEntityManagers() {
        val book = bookRepository.save("The Stand")
        assertNotNull(book.id)
        assertEquals("The Stand", bookRepository.findById(book.id!!)!!.title)
        assertNull(bookRepository.findInOtherById(book.id!!))

        val other = bookRepository.saveToOther("It")
        assertNotNull(other.id)
        assertEquals("It", bookRepository.findInOtherById(other.id!!)!!.title)
    }
}
