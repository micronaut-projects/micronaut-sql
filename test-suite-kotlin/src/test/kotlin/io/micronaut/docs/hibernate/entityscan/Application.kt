package io.micronaut.docs.hibernate.entityscan

// tag::imports[]
import io.micronaut.core.annotation.Introspected
import jakarta.persistence.Entity
// end::imports[]

// tag::clazz[]
@Introspected(packages = ["io.micronaut.docs.hibernate.entityscan.external"], includedAnnotations = [Entity::class]) // <1>
class Application
// end::clazz[]
