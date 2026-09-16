from typing import Annotated

from jakarta.inject import Inject
from micronaut.core.beans import BeanIntrospector
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.hibernate import SessionFactory
from org.junit.jupiter.api import Disabled, Test

from micronaut.docs.hibernate.entityscan.external.Product import Product


# TODO(python): the Python compiler does not emit the JPA annotations (@Entity, @Id, @GeneratedValue, ...) of a
# Python class on the generated Java class, so Hibernate does not recognise Python classes as entities
# ("Unknown entity type"). See micronaut/docs/DISABLED_TESTS.md.
@Disabled("TODO(python): JPA annotations of Python classes are not emitted on the generated Java class")
@MicronautTest(transactional=False)
class EntityScanTest:
    session_factory: Annotated[SessionFactory, Inject]

    @Test
    def test_introspected_package_is_scanned(self):
        assert BeanIntrospector.SHARED.findIntrospection(Product).isPresent()
        assert self.session_factory.getMetamodel().entity(Product).getJavaType() == Product
