package io.micronaut.configuration.hibernate.jpa.proxy;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class Customer {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    @ManyToOne(fetch = FetchType.LAZY)
    private DepartmentCompositeId departmentCompositeId;
    @ManyToOne(fetch = FetchType.LAZY)
    private DepartmentSimpleWithEquals departmentSimpleWithEquals;
    @ManyToOne(fetch = FetchType.LAZY)
    private DepartmentSimpleWithoutEquals departmentSimpleWithoutEquals;

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

    public DepartmentCompositeId getDepartmentCompositeId() {
        return departmentCompositeId;
    }

    public void setDepartmentCompositeId(DepartmentCompositeId departmentCompositeId) {
        this.departmentCompositeId = departmentCompositeId;
    }

    public DepartmentSimpleWithEquals getDepartmentSimpleWithEquals() {
        return departmentSimpleWithEquals;
    }

    public void setDepartmentSimpleWithEquals(DepartmentSimpleWithEquals departmentSimpleWithEquals) {
        this.departmentSimpleWithEquals = departmentSimpleWithEquals;
    }

    public DepartmentSimpleWithoutEquals getDepartmentSimpleWithoutEquals() {
        return departmentSimpleWithoutEquals;
    }

    public void setDepartmentSimpleWithoutEquals(DepartmentSimpleWithoutEquals departmentSimpleWithoutEquals) {
        this.departmentSimpleWithoutEquals = departmentSimpleWithoutEquals;
    }
}
