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
open class Owner {

    @Id
    @GeneratedValue
    open var id: Long? = null

    open var name: String? = null
}
// end::clazz[]
