package io.micronaut.docs.hibernate.proxies

// tag::imports[]
import io.micronaut.configuration.hibernate.jpa.proxy.GenerateProxy
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
// end::imports[]

// tag::clazz[]
@Entity
@GenerateProxy
class Owner {

    @Id
    @GeneratedValue
    Long id

    String name
}
// end::clazz[]
