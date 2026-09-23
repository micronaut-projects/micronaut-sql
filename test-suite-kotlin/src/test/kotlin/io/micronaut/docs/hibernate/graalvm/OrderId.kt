package io.micronaut.docs.hibernate.graalvm

// tag::imports[]
import io.micronaut.core.annotation.ReflectiveAccess
import jakarta.persistence.Embeddable
import java.io.Serializable
// end::imports[]

// tag::clazz[]
@Embeddable
@ReflectiveAccess
data class OrderId(var region: String? = null, var number: Long? = null) : Serializable
// end::clazz[]
