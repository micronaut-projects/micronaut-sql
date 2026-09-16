package io.micronaut.docs.hibernate.session;

// tag::imports[]
import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
// end::imports[]

// tag::clazz[]
@Singleton
public class BookRepository {

    @PersistenceContext
    EntityManager entityManager; // <1>

    @PersistenceContext(name = "other")
    EntityManager otherManager; // <2>

    @Transactional // <3>
    public Book save(String title) {
        Book book = new Book(title);
        entityManager.persist(book);
        return book;
    }

    @Transactional("other") // <4>
    public Book saveToOther(String title) {
        Book book = new Book(title);
        otherManager.persist(book);
        return book;
    }

    @Transactional(readOnly = true)
    public Book findById(Long id) {
        return entityManager.find(Book.class, id);
    }

    @Transactional(value = "other", readOnly = true)
    public Book findInOtherById(Long id) {
        return otherManager.find(Book.class, id);
    }
}
// end::clazz[]
