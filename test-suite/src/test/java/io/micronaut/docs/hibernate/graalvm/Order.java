package io.micronaut.docs.hibernate.graalvm;

// tag::imports[]
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
// end::imports[]

// tag::clazz[]
@Entity
@Table(name = "orders")
public class Order {

    @EmbeddedId
    private OrderId id;

    private String customer;

    public OrderId getId() {
        return id;
    }

    public void setId(OrderId id) {
        this.id = id;
    }

    public String getCustomer() {
        return customer;
    }

    public void setCustomer(String customer) {
        this.customer = customer;
    }
}
// end::clazz[]
