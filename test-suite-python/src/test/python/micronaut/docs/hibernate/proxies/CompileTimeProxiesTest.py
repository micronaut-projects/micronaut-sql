from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Disabled, Test

from micronaut.docs.hibernate.proxies.PetRepository import PetRepository


# TODO(python): the Python compiler does not emit the JPA annotations (@Entity, @Id, @GeneratedValue, ...) of a
# Python class on the generated Java class, so Hibernate does not recognise Python classes as entities
# ("Unknown entity type"). See micronaut/docs/DISABLED_TESTS.md.
@Disabled("TODO(python): JPA annotations of Python classes are not emitted on the generated Java class")
@MicronautTest(transactional=False)
class CompileTimeProxiesTest:
    pet_repository: Annotated[PetRepository, Inject]

    @Test
    def test_owner_is_a_compile_time_proxy(self):
        pet = self.pet_repository.save("Dino", "Fred")
        assert self.pet_repository.is_owner_lazily_loaded(pet.id)
        assert "Intercepted" in self.pet_repository.owner_class_name(pet.id)
