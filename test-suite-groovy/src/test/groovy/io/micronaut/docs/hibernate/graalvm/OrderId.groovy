package io.micronaut.docs.hibernate.graalvm

// tag::imports[]
import groovy.transform.EqualsAndHashCode
import io.micronaut.core.annotation.ReflectiveAccess
import jakarta.persistence.Embeddable
// end::imports[]

// tag::clazz[]
@Embeddable
@ReflectiveAccess
@EqualsAndHashCode
class OrderId implements Serializable {

    String region
    Long number

    OrderId() {
    }

    OrderId(String region, Long number) {
        this.region = region
        this.number = number
    }
}
// end::clazz[]
