package io.micronaut.docs.hibernate.proxies

import io.micronaut.transaction.annotation.Transactional
import jakarta.inject.Singleton
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.hibernate.Hibernate

@Singleton
open class PetRepository {

    @PersistenceContext
    lateinit var entityManager: EntityManager

    @Transactional
    open fun save(petName: String, ownerName: String): Pet {
        val owner = Owner()
        owner.name = ownerName
        entityManager.persist(owner)
        val pet = Pet()
        pet.name = petName
        pet.owner = owner
        entityManager.persist(pet)
        return pet
    }

    @Transactional(readOnly = true)
    open fun isOwnerLazilyLoaded(petId: Long): Boolean {
        val pet = entityManager.find(Pet::class.java, petId)
        val owner = pet.owner!!
        val initialized = Hibernate.isInitialized(owner)
        owner.name
        return !initialized && Hibernate.isInitialized(owner)
    }

    @Transactional(readOnly = true)
    open fun ownerClass(petId: Long): Class<*> = entityManager.find(Pet::class.java, petId).owner!!.javaClass
}
