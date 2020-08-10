package io.micronaut.configuration.hibernate.jpa.mapping

import io.micronaut.context.ApplicationContext
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.jaxb.SourceType
import org.hibernate.boot.jaxb.hbm.spi.JaxbHbmHibernateMapping
import spock.lang.Specification

class MappingResourcesSpec extends Specification {

    void "test custom mapping resources initialization"() {
        given:
        def context = ApplicationContext.run(
                'datasources.default.name': 'test',
                'jpa.default.entity-scan.packages': ['io.micronaut.configuration.hibernate.jpa.mapping'],
                'jpa.default.properties.hibernate.hbm2ddl.auto': 'create-drop',
                'jpa.default.mapping-resources': ['hibernate/custom.hbm.xml']
        )

        when:
        MetadataSources metadataSources = context.getBean(MetadataSources)

        then:
        metadataSources.xmlBindings
        metadataSources.xmlBindings.size() == 1
        metadataSources.xmlBindings[0].root instanceof JaxbHbmHibernateMapping
        metadataSources.xmlBindings[0].origin.type == SourceType.RESOURCE
        metadataSources.xmlBindings[0].origin.name == 'hibernate/custom.hbm.xml'

        when:
        AccountRepository accountRepository = context.getBean(AccountRepository)
        accountRepository.create("test", "test")
        Account testAccount = accountRepository.findByUsername("test")

        then:
        testAccount
        testAccount.username == "test"

        cleanup:
        context.close()
    }
}
