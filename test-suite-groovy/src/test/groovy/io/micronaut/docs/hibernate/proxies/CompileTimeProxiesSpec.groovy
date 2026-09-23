package io.micronaut.docs.hibernate.proxies

import io.micronaut.configuration.hibernate.jpa.proxy.IntroducedHibernateProxy
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Ignore
import spock.lang.Specification

// TODO(micronaut-core): inject-groovy (GroovyClassElement) skips methods whose name starts with "$", so the
// generated Groovy $Owner$Intercepted proxy does not implement IntroducedHibernateProxy.$registerInterceptor
// and Hibernate fails with an AbstractMethodError when the lazy association is loaded. Java and Kotlin work.
@Ignore("@GenerateProxy is not supported for Groovy entities: inject-groovy skips \$-prefixed introduced methods")
@MicronautTest(transactional = false)
class CompileTimeProxiesSpec extends Specification {

    @Inject
    PetRepository petRepository

    void "test owner is a compile time proxy"() {
        when:
        Pet pet = petRepository.save("Dino", "Fred")

        then:
        petRepository.isOwnerLazilyLoaded(pet.id)
        IntroducedHibernateProxy.isAssignableFrom(petRepository.ownerClass(pet.id))
    }
}
