package io.micronaut.docs.hibernate.graalvm

import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@MicronautTest(transactional = false)
class EmbeddedIdSpec extends Specification {

    @Inject
    OrderRepository orderRepository

    void "test embedded id"() {
        given:
        OrderId id = new OrderId("EU", 42L)

        when:
        orderRepository.save(id, "Fred")
        Order order = orderRepository.findById(new OrderId("EU", 42L))

        then:
        order.customer == "Fred"
        order.id == id
    }
}
