package io.micronaut.docs.hibernate.session

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class Book {

    @Id
    @GeneratedValue
    Long id

    String title

    Book() {
    }

    Book(String title) {
        this.title = title
    }
}
