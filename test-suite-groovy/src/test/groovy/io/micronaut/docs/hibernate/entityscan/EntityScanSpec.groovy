package io.micronaut.docs.hibernate.entityscan

import io.micronaut.core.beans.BeanIntrospector
import io.micronaut.docs.hibernate.entityscan.external.Product
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.hibernate.SessionFactory
import spock.lang.Specification

@MicronautTest(transactional = false)
class EntityScanSpec extends Specification {

    @Inject
    SessionFactory sessionFactory

    void "test introspected package is scanned"() {
        expect:
        BeanIntrospector.SHARED.findIntrospection(Product).isPresent()
        sessionFactory.metamodel.entity(Product).javaType == Product
    }
}
