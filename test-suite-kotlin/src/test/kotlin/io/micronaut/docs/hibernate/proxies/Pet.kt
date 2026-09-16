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
open class Pet {

    @Id
    @GeneratedValue
    open var id: Long? = null

    open var name: String? = null

    @ManyToOne(fetch = FetchType.LAZY)
    open var owner: Owner? = null
}
// end::clazz[]
