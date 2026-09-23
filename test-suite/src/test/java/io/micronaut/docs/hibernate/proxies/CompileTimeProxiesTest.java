package io.micronaut.docs.hibernate.proxies;

import io.micronaut.configuration.hibernate.jpa.proxy.IntroducedHibernateProxy;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest(transactional = false)
class CompileTimeProxiesTest {

    @Inject
    PetRepository petRepository;

    @Test
    void testOwnerIsACompileTimeProxy() {
        Pet pet = petRepository.save("Dino", "Fred");
        assertTrue(petRepository.isOwnerLazilyLoaded(pet.getId()));
        assertTrue(IntroducedHibernateProxy.class.isAssignableFrom(petRepository.ownerClass(pet.getId())));
    }
}
