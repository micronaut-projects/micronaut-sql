/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
