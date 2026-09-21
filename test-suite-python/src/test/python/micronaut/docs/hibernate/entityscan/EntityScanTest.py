from typing import Annotated

from jakarta.inject import Inject
from micronaut.core.beans import BeanIntrospector
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.hibernate import SessionFactory
from org.junit.jupiter.api import Test

from micronaut.docs.hibernate.entityscan.external.Product import Product


@MicronautTest(transactional=False)
class EntityScanTest:
    session_factory: Annotated[SessionFactory, Inject]

    @Test
    def test_introspected_package_is_scanned(self):
        assert BeanIntrospector.SHARED.findIntrospection(Product).isPresent()
        assert self.session_factory.getMetamodel().entity(Product).getJavaType().getName() == "micronaut.docs.hibernate.entityscan.external.Product"
