package io.micronaut.docs.hibernate.proxies;

import io.micronaut.transaction.annotation.Transactional;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Hibernate;

@Singleton
public class PetRepository {

    @PersistenceContext
    EntityManager entityManager;

    @Transactional
    public Pet save(String petName, String ownerName) {
        Owner owner = new Owner();
        owner.setName(ownerName);
        entityManager.persist(owner);
        Pet pet = new Pet();
        pet.setName(petName);
        pet.setOwner(owner);
        entityManager.persist(pet);
        return pet;
    }

    @Transactional(readOnly = true)
    public boolean isOwnerLazilyLoaded(Long petId) {
        Pet pet = entityManager.find(Pet.class, petId);
        Owner owner = pet.getOwner();
        boolean initialized = Hibernate.isInitialized(owner);
        owner.getName();
        return !initialized && Hibernate.isInitialized(owner);
    }

    @Transactional(readOnly = true)
    public Class<?> ownerClass(Long petId) {
        return entityManager.find(Pet.class, petId).getOwner().getClass();
    }
}
