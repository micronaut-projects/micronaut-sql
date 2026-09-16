package io.micronaut.docs.hibernate.graalvm;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Singleton
public class OrderRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public Order save(OrderId id, String customer) {
        Order order = new Order();
        order.setId(id);
        order.setCustomer(customer);
        entityManager.persist(order);
        return order;
    }

    @Transactional(readOnly = true)
    public Order findById(OrderId id) {
        return entityManager.find(Order.class, id);
    }
}
