package io.micronaut.docs.hibernate.proxies;

// tag::imports[]
import io.micronaut.configuration.hibernate.jpa.proxy.GenerateProxy;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
// end::imports[]

// tag::clazz[]
@Entity
@GenerateProxy
public class Owner {

    @Id
    @GeneratedValue
    private Long id;

    private String name;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
// end::clazz[]
