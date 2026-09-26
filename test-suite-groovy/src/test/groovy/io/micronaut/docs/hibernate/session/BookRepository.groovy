package io.micronaut.docs.hibernate.session

// tag::imports[]
import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
// end::imports[]

// tag::clazz[]
@Singleton
class BookRepository {

    @PersistenceContext
    EntityManager entityManager // <1>

    @PersistenceContext(name = "other")
    EntityManager otherManager // <2>

    @Transactional // <3>
    Book save(String title) {
        Book book = new Book(title)
        entityManager.persist(book)
        return book
    }

    @Transactional("other") // <4>
    Book saveToOther(String title) {
        Book book = new Book(title)
        otherManager.persist(book)
        return book
    }

    @Transactional(readOnly = true)
    Book findById(Long id) {
        entityManager.find(Book, id)
    }

    @Transactional(value = "other", readOnly = true)
    Book findInOtherById(Long id) {
        otherManager.find(Book, id)
    }
}
// end::clazz[]
