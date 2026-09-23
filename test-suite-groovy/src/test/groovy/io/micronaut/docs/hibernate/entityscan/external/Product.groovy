package io.micronaut.docs.hibernate.entityscan.external

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id

@Entity
class Product {

    @Id
    @GeneratedValue
    Long id

    String name
}
