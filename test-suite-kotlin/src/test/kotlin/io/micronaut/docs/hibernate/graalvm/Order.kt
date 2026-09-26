package io.micronaut.docs.hibernate.graalvm

// tag::imports[]
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
// end::imports[]

// tag::clazz[]
@Entity
@Table(name = "orders")
open class Order {

    @EmbeddedId
    open var id: OrderId? = null

    open var customer: String? = null
}
// end::clazz[]
