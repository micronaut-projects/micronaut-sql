package io.micronaut.docs.hibernate.graalvm;

// tag::imports[]
import io.micronaut.core.annotation.ReflectiveAccess;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
// end::imports[]

// tag::clazz[]
@Embeddable
@ReflectiveAccess
public class OrderId implements Serializable {

    private String region;
    private Long number;

    public OrderId() {
    }

    public OrderId(String region, Long number) {
        this.region = region;
        this.number = number;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof OrderId other && Objects.equals(region, other.region) && Objects.equals(number, other.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(region, number);
    }
}
// end::clazz[]
