package io.micronaut.docs.hibernate.graalvm

import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class EmbeddedIdTest {

    @Inject
    lateinit var orderRepository: OrderRepository

    @Test
    fun testEmbeddedId() {
        val id = OrderId("EU", 42L)
        orderRepository.save(id, "Fred")
        val order = orderRepository.findById(OrderId("EU", 42L))!!
        assertEquals("Fred", order.customer)
        assertEquals(id, order.id)
    }
}
