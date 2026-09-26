package io.micronaut.docs.hibernate.graalvm

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

@Singleton
open class OrderRepository {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Transactional
    open fun save(id: OrderId, customer: String): Order {
        val order = Order()
        order.id = id
        order.customer = customer
        entityManager.persist(order)
        return order
    }

    @Transactional(readOnly = true)
    open fun findById(id: OrderId): Order? = entityManager.find(Order::class.java, id)
}
