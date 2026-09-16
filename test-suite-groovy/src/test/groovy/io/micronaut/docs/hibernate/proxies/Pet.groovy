package io.micronaut.docs.hibernate.proxies

// tag::imports[]
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.ManyToOne
// end::imports[]

// tag::clazz[]
@Entity
class Pet {

    @Id
    @GeneratedValue
    Long id

    String name

    @ManyToOne(fetch = FetchType.LAZY)
    Owner owner
}
// end::clazz[]
