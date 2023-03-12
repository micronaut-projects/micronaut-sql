/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.hibernate6.jpa

import io.micrometer.core.instrument.FunctionCounter
import io.micrometer.core.instrument.MeterRegistry
import io.micronaut.context.ApplicationContext
import io.micronaut.context.env.Environment
import io.micronaut.http.exceptions.HttpException
import io.micronaut.transaction.TransactionDefinition
import io.micronaut.transaction.annotation.TransactionalAdvice
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.hibernate.Session
import spock.lang.AutoCleanup
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

import jakarta.validation.ConstraintViolation
import jakarta.validation.ConstraintViolationException
import jakarta.validation.constraints.NotBlank

/**
 * @author graemerocher
 * @since 1.0
 */
class JpaSetupSpec extends Specification {

    @Shared @AutoCleanup ApplicationContext applicationContext = ApplicationContext.run(
            'datasources.default.name':'mydb',
            'jpa.default.properties.hibernate.hbm2ddl.auto':'create-drop',
            'jpa.default.properties.hibernate.generate_statistics':true,
            'micronaut.metrics.binders.hibernate.tags.some':'bar'
    )

    // TODO: Temporary ignore since validation class jakarta.validation.ConstraintViolation is not available
    @Ignore
    void "test setup entity manager with validation"() {
        when:
        EntityManagerFactory entityManagerFactory = applicationContext.getBean(EntityManagerFactory)

        then:
        entityManagerFactory != null

        when:
        EntityManager em = entityManagerFactory.createEntityManager()
        def tx = em.getTransaction()
        tx.begin()
        em.persist(new Book(title: ""))
        em.flush()
        tx.commit()

        then:
        thrown(ConstraintViolationException)

    }

    void "test setup entity manager save entity"() {
        when:
        EntityManagerFactory entityManagerFactory = applicationContext.getBean(EntityManagerFactory)

        then:
        entityManagerFactory != null

        when:
        EntityManager em = entityManagerFactory.createEntityManager()
        def tx = em.getTransaction()
        tx.begin()
        em.persist(new Book(title: "The Stand"))
        em.flush()

        then:
        em.createQuery("select book from Book book").resultList.size() == 1
        em.createNativeQuery("select * from book", Book).resultList.size() == 1

        when:
        MeterRegistry meterRegistry = applicationContext.getBean(MeterRegistry)
        FunctionCounter c = meterRegistry.get("hibernate.query.executions").tag("entityManagerFactory", JpaConfiguration.PRIMARY).functionCounter()

        then:
        c.count() > 0

        cleanup:
        tx.rollback()
    }

    void "test transaction management"() {
        given:
        BookService bookService = applicationContext.getBean(BookService)

        when:
        List<Book> books = bookService.listBooks()

        then:
        books.size() == 0

        when:
        bookService.saveError()

        then:
        def e  = thrown(Exception)

        when:
        books = bookService.listBooks()
        then:
        books.size() == 0

        when:
        bookService.saveSuccess()
        books = bookService.listBooks()

        then:
        books.size() == 1
        books[0].title == 'THE STAND'
    }

    void "test inject java persistence context"() {
        given:
        JavaBookService bookService = applicationContext.getBean(JavaBookService)
        JavaReadOnlyBookService readOnlyBookService = applicationContext.getBean(JavaReadOnlyBookService)

        expect:
        bookService.testFieldInject()
        bookService.testMethodInject()
        bookService.testNativeQuery()
        readOnlyBookService.testNativeQuery()
        bookService.testClose()
        bookService.testCloseWithoutTx()
    }
}

@Entity
class Book {

    @Id
    @GeneratedValue
    Long id

    @NotBlank
    @Convert(converter = UppercaseConverter.class)
    String title
}

@Singleton
class UppercaseConverter implements AttributeConverter<String, String> {

    private final Environment environment

    UppercaseConverter(Environment environment) {
        this.environment = environment
    }

    @Override
    String convertToDatabaseColumn(String attribute) {
        return attribute.toUpperCase(Locale.ENGLISH)
    }

    @Override
    String convertToEntityAttribute(String dbData) {
        return dbData
    }
}

@Singleton
class BookService {

    @Inject
    Session session

    @TransactionalAdvice(
            readOnly = true,
            propagation = TransactionDefinition.Propagation.MANDATORY,
            isolation = TransactionDefinition.Isolation.REPEATABLE_READ,
            transactionManager = "foo",
            timeout = 1000,
            noRollbackFor = HttpException
    )
    void testMethod() {

    }

    @TransactionalAdvice(readOnly = true)
    List<Book> listBooks() {
        def query = session.getCriteriaBuilder().createQuery(Book)
        def root = query.from(Book)
        query.select(root)
        return session.createQuery(query).getResultList()
    }

    @TransactionalAdvice(readOnly = true)
    List<Book> saveReadOnly() {
        session.persist(new Book(title: "the stand"))
        listBooks()
    }

    @TransactionalAdvice
    List<Book> saveError() {
        session.persist(new Book(title: "the stand"))
        throw new Exception("bad things happened")
    }

    @TransactionalAdvice
    List<Book> saveSuccess() {
        session.persist(new Book(title: "the stand"))
        listBooks()
    }

}
