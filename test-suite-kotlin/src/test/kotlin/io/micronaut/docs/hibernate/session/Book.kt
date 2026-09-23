package io.micronaut.docs.hibernate.session

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
open class Book(open var title: String? = null) {

    @Id
    @GeneratedValue
    open var id: Long? = null
}
