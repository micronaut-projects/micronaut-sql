package io.micronaut.docs.hibernate.entityscan;

import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.docs.hibernate.entityscan.external.Product;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
class EntityScanTest {

    @Inject
    SessionFactory sessionFactory;

    @Test
    void testIntrospectedPackageIsScanned() {
        assertTrue(BeanIntrospector.SHARED.findIntrospection(Product.class).isPresent());
        assertEquals(Product.class, sessionFactory.getMetamodel().entity(Product.class).getJavaType());
    }
}
