from typing import Annotated

from jakarta.inject import Inject
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Disabled, Test

from micronaut.docs.hibernate.proxies.PetRepository import PetRepository


@Disabled("TODO(python): the id Hibernate assigns on persist is set on the Java wrapper of the Python entity and not written back to the Python object (pet.id stays None), see DISABLED_TESTS.md")
@MicronautTest(transactional=False)
class CompileTimeProxiesTest:
    pet_repository: Annotated[PetRepository, Inject]

    @Test
    def test_owner_is_a_compile_time_proxy(self):
        pet = self.pet_repository.save("Dino", "Fred")
        assert self.pet_repository.is_owner_lazily_loaded(pet.id)
        assert "Intercepted" in self.pet_repository.owner_class_name(pet.id)
