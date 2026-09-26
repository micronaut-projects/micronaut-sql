package io.micronaut.docs.hibernate.graalvm

// tag::imports[]
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
// end::imports[]

// tag::clazz[]
@Entity
@Table(name = "orders")
class Order {

    @EmbeddedId
    OrderId id

    String customer
}
// end::clazz[]
