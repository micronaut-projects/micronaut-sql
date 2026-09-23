package io.micronaut.docs.hibernate.session

// tag::imports[]
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
// end::imports[]

// tag::clazz[]
@Singleton
open class BookRepository {

    @PersistenceContext
    lateinit var entityManager: EntityManager // <1>

    @PersistenceContext(name = "other")
    lateinit var otherManager: EntityManager // <2>

    @Transactional // <3>
    open fun save(title: String): Book {
        val book = Book(title)
        entityManager.persist(book)
        return book
    }

    @Transactional("other") // <4>
    open fun saveToOther(title: String): Book {
        val book = Book(title)
        otherManager.persist(book)
        return book
    }

    @Transactional(readOnly = true)
    open fun findById(id: Long): Book? = entityManager.find(Book::class.java, id)

    @Transactional(value = "other", readOnly = true)
    open fun findInOtherById(id: Long): Book? = otherManager.find(Book::class.java, id)
}
// end::clazz[]
