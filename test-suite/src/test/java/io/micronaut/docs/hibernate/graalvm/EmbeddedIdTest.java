package io.micronaut.docs.hibernate.graalvm;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(transactional = false)
class EmbeddedIdTest {

    @Inject
    OrderRepository orderRepository;

    @Test
    void testEmbeddedId() {
        OrderId id = new OrderId("EU", 42L);
        orderRepository.save(id, "Fred");
        Order order = orderRepository.findById(new OrderId("EU", 42L));
        assertEquals("Fred", order.getCustomer());
        assertEquals(id, order.getId());
    }
}
