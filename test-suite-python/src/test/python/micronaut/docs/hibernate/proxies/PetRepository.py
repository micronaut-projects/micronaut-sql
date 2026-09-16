from typing import Annotated

from jakarta.inject import Singleton
from jakarta.persistence import EntityManager, PersistenceContext
from micronaut.transaction.annotation import Transactional
from org.hibernate import Hibernate

from micronaut.docs.hibernate.proxies.Owner import Owner
from micronaut.docs.hibernate.proxies.Pet import Pet


@Singleton
class PetRepository:
    entity_manager: Annotated[EntityManager, PersistenceContext]

    @Transactional
    def save(self, pet_name: str, owner_name: str) -> Pet:
        owner = Owner()
        owner.name = owner_name
        self.entity_manager.persist(owner)
        pet = Pet()
        pet.name = pet_name
        pet.owner = owner
        self.entity_manager.persist(pet)
        return pet

    @Transactional(readOnly=True)
    def is_owner_lazily_loaded(self, pet_id: int) -> bool:
        pet = self.entity_manager.find(Pet, pet_id)
        owner = pet.owner
        initialized = Hibernate.isInitialized(owner)
        owner.name
        return not initialized and Hibernate.isInitialized(owner)

    @Transactional(readOnly=True)
    def owner_class_name(self, pet_id: int) -> str:
        return self.entity_manager.find(Pet, pet_id).owner.getClass().getName()
