package io.micronaut.docs.hibernate.entityscan

import io.micronaut.core.beans.BeanIntrospector
import io.micronaut.docs.hibernate.entityscan.external.Product
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class EntityScanTest {

    @Inject
    lateinit var sessionFactory: SessionFactory

    @Test
    fun testIntrospectedPackageIsScanned() {
        assertTrue(BeanIntrospector.SHARED.findIntrospection(Product::class.java).isPresent)
        assertEquals(Product::class.java, sessionFactory.metamodel.entity(Product::class.java).javaType)
    }
}
