package io.micronaut.docs.hibernate.proxies

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.Hibernate

@Singleton
class PetRepository {

    @PersistenceContext
    EntityManager entityManager

    @Transactional
    Pet save(String petName, String ownerName) {
        Owner owner = new Owner(name: ownerName)
        entityManager.persist(owner)
        Pet pet = new Pet(name: petName, owner: owner)
        entityManager.persist(pet)
        return pet
    }

    @Transactional(readOnly = true)
    boolean isOwnerLazilyLoaded(Long petId) {
        Pet pet = entityManager.find(Pet, petId)
        Owner owner = pet.owner
        boolean initialized = Hibernate.isInitialized(owner)
        owner.name
        return !initialized && Hibernate.isInitialized(owner)
    }

    @Transactional(readOnly = true)
    Class<?> ownerClass(Long petId) {
        entityManager.find(Pet, petId).owner.getClass()
    }
}
