package io.micronaut.configuration.hibernate6.jpa.proxy;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.io.Serializable;
import java.util.Set;

@Entity
@GenerateProxy
public class DepartmentCompositeId implements Serializable {

    @Id
    private String a;
    @Id
    private String b;
    private String name;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "departmentCompositeId")
    private Set<Customer> customers;

    public String getA() {
        return a;
    }

    public void setA(String a) {
        this.a = a;
    }

    public String getB() {
        return b;
    }

    public void setB(String b) {
        this.b = b;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Customer> getCustomers() {
        return customers;
    }

    public void setCustomers(Set<Customer> customers) {
        this.customers = customers;
    }

}
