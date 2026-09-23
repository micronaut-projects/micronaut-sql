package io.micronaut.docs.hibernate.graalvm

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext

@Singleton
class OrderRepository {

    @PersistenceContext
    EntityManager entityManager

    @Transactional
    Order save(OrderId id, String customer) {
        Order order = new Order(id: id, customer: customer)
        entityManager.persist(order)
        return order
    }

    @Transactional(readOnly = true)
    Order findById(OrderId id) {
        entityManager.find(Order, id)
    }
}
