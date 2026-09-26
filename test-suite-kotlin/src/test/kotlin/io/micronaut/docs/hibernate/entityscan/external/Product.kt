package io.micronaut.docs.hibernate.entityscan.external

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
open class Product {

    @Id
    @GeneratedValue
    open var id: Long? = null

    open var name: String? = null
}
