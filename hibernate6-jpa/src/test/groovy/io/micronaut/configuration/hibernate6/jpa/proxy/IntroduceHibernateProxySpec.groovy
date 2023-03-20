/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.hibernate6.jpa.proxy

import io.micronaut.aop.Introduced
import io.micronaut.context.ApplicationContext
import org.hibernate.proxy.HibernateProxy
import spock.lang.AutoCleanup
import spock.lang.Shared
import spock.lang.Specification

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory

class IntroduceHibernateProxySpec extends Specification {

    @Shared
    @AutoCleanup
    ApplicationContext applicationContext = ApplicationContext.run(
            'datasources.default.name': 'mydb',
            'jpa.default.properties.hibernate.hbm2ddl.auto': 'create-drop',
            'jpa.default.compile-time-hibernate-proxies': true
    )

    void "test customer with simple lazy with equals"() {
        when:
            EntityManagerFactory entityManagerFactory = applicationContext.getBean(EntityManagerFactory)

        then:
            entityManagerFactory != null

        when:
            EntityManager em = entityManagerFactory.createEntityManager()
            def tx = em.getTransaction()
            tx.begin()
            def newDepartment = new DepartmentSimpleWithEquals(name: "Xyz")
            em.persist(newDepartment)
            def newCustomer = new Customer(name: "Joe")
            newCustomer.setDepartmentSimpleWithEquals(newDepartment)
            em.persist(newCustomer)
            em.flush()
            em.clear()

        then:
            def customer = em.find(Customer, newCustomer.getId())
            def department = customer.getDepartmentSimpleWithEquals()
            department instanceof Introduced
            department instanceof HibernateProxy
            department instanceof IntroducedHibernateProxy
            def lazyInitializer = ((HibernateProxy) department).getHibernateLazyInitializer()
            lazyInitializer.isUninitialized()
            department.getId() // call to identifier method shouldn't init proxy
            department.getId() == newDepartment.getId()
            lazyInitializer.isUninitialized()
            // hash code or equals are implemented -> should trigger lazy init
            department.hashCode() == department.hashCode() // Init proxy
            !lazyInitializer.isUninitialized()
            department.hashCode() == newDepartment.hashCode()
            department.equals(department)
            department.equals(newDepartment)
            newDepartment.equals(department)
            !department.equals(new DepartmentSimpleWithEquals(id: 123L))
            !new DepartmentSimpleWithEquals(id: 123L).equals(department)
            department.getName()
            department.getName() == newDepartment.getName()

        cleanup:
            tx.rollback()
    }

    void "test customer with simple lazy without equals"() {
        when:
            EntityManagerFactory entityManagerFactory = applicationContext.getBean(EntityManagerFactory)

        then:
            entityManagerFactory != null

        when:
            EntityManager em = entityManagerFactory.createEntityManager()
            def tx = em.getTransaction()
            tx.begin()
            def newDepartment = new DepartmentSimpleWithoutEquals(name: "Xyz")
            em.persist(newDepartment)
            def newCustomer = new Customer(name: "Joe")
            newCustomer.setDepartmentSimpleWithoutEquals(newDepartment)
            em.persist(newCustomer)
            em.flush()
            em.clear()

        then:
            def customer = em.find(Customer, newCustomer.getId())
            def department = customer.getDepartmentSimpleWithoutEquals()
            department instanceof Introduced
            department instanceof HibernateProxy
            department instanceof IntroducedHibernateProxy
            def lazyInitializer = ((HibernateProxy) department).getHibernateLazyInitializer()
            lazyInitializer.isUninitialized()
            department.getId() // call to identifier method shouldn't init proxy
            department.getId() == newDepartment.getId()
            department.hashCode() != newDepartment.hashCode()
            department.equals(department) // proxy equals proxy
            !department.equals(newDepartment) // proxy not equals no-proxy
            lazyInitializer.isUninitialized()
            // hashCode or equals aren't implemented -> shouldn't match and shouldn't trigger lazy init
            department.getName() // Init proxy
            !lazyInitializer.isUninitialized()
            department.getName() == newDepartment.getName()

        cleanup:
            tx.rollback()
    }

    // test for componentIdType.isMethodOf(executableMethod.getTargetMethod())
    void "test customer with composite id"() {
        when:
            EntityManagerFactory entityManagerFactory = applicationContext.getBean(EntityManagerFactory)

        then:
            entityManagerFactory != null

        when:
            EntityManager em = entityManagerFactory.createEntityManager()
            def tx = em.getTransaction()
            tx.begin()
            def departmentComposite = new DepartmentCompositeId(a: "xx", b: "yy", name: "Xyz")
            em.persist(departmentComposite)
            def newCustomer = new Customer(name: "Joe")
            newCustomer.setDepartmentCompositeId(departmentComposite)
            em.persist(newCustomer)
            em.flush()
            em.clear()

        then:
            def customer = em.find(Customer, newCustomer.getId())
            def depComposite = customer.getDepartmentCompositeId()
            depComposite instanceof Introduced
            depComposite instanceof HibernateProxy
            depComposite instanceof IntroducedHibernateProxy
            def lazyInitializer = ((HibernateProxy) depComposite).getHibernateLazyInitializer()
            lazyInitializer.isUninitialized()
            depComposite.getA() // call to identifier method shouldn't init proxy
            depComposite.getB() // call to identifier method shouldn't init proxy
            depComposite.getA() == departmentComposite.getA()
            depComposite.getB() == departmentComposite.getB()
            depComposite.hashCode() // hashcode of the proxy
            depComposite.equals(depComposite) // equals of the proxy
            !depComposite.equals(departmentComposite)
            lazyInitializer.isUninitialized()
            // hashCode or equals aren't implemented -> shouldn't match and shouldn't trigger lazy init
            depComposite.getName()
            !lazyInitializer.isUninitialized()
            depComposite.getName() == departmentComposite.getName()
        cleanup:
            tx.rollback()
    }
}


