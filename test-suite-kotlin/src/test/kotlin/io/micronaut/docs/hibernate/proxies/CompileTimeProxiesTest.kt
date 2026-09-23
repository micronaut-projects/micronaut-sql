package io.micronaut.docs.hibernate.proxies

import io.micronaut.configuration.hibernate.jpa.proxy.IntroducedHibernateProxy
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@MicronautTest(transactional = false)
class CompileTimeProxiesTest {

    @Inject
    lateinit var petRepository: PetRepository

    @Test
    fun testOwnerIsACompileTimeProxy() {
        val pet = petRepository.save("Dino", "Fred")
        assertTrue(petRepository.isOwnerLazilyLoaded(pet.id!!))
        assertTrue(IntroducedHibernateProxy::class.java.isAssignableFrom(petRepository.ownerClass(pet.id!!)))
    }
}
