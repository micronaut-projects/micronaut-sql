package io.micronaut.configuration.hibernate.jpa.datasources.db2;


import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(name = "bookstore")
public class Bookstore {

    public Bookstore() {}

    public Bookstore(@NotNull String name) {
        this.name = name;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @Column(name = "name", nullable = false)
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
