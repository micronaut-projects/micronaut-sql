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
//file:noinspection GroovyAccessibility
package io.micronaut.configuration.jdbc.dbcp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.DefaultApplicationContext
import io.micronaut.context.env.MapPropertySource
import spock.lang.Specification

class GlobalDatasourcePropertiesSpec extends Specification {

    void "test no global datasource configuration exists when no global properties are present"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.url': 'jdbc:h2:mem:default']
        ))
        applicationContext.start()

        when:
        Optional<GlobalDatasourceProperties> properties = applicationContext.findBean(GlobalDatasourceProperties)
        Optional<DatasourceConfiguration> datasourceConfig = applicationContext.findBean(DatasourceConfiguration)

        then: "No global beans are created when no global configuration is present"
        datasourceConfig.isPresent()
        datasourceConfig.get().connectionProperties.isEmpty()
        datasourceConfig.get().url == 'jdbc:h2:mem:default'
        properties.isEmpty()

        cleanup:
        applicationContext.close()
    }

    void "test global datasource properties configuration creates correct beans"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['global.datasources.data-source-properties.ApplicationName': 'MyApp',
                 'global.datasources.data-source-properties.assumeMinServerVersion': '9.0',
                 'global.datasources.data-source-properties.reWriteBatchInserts': true]
        ))
        applicationContext.start()

        when:
        GlobalDatasourceProperties properties = applicationContext.getBean(GlobalDatasourceProperties)

        then: "GlobalDatasourceProperties bean is created with correct properties"
        properties != null
        properties.dataSourceProperties != null
        properties.dataSourceProperties.size() == 3
        properties.dataSourceProperties['ApplicationName'] == 'MyApp'
        properties.dataSourceProperties['assumeMinServerVersion'] == '9.0'
        properties.dataSourceProperties['reWriteBatchInserts'] == "true"

        and: "GlobalDatasourceConfigModifier bean is also created"
        applicationContext.containsBean(GlobalDatasourceConfigModifier)

        cleanup:
        applicationContext.close()
    }

    void "test global properties are applied to all datasource configurations"() {
        given:
        ApplicationContext applicationContext = new DefaultApplicationContext("test")
        applicationContext.environment.addPropertySource(MapPropertySource.of(
                'test',
                ['datasources.default.url': 'jdbc:h2:mem:default',
                 'datasources.secondary.url': 'jdbc:h2:mem:secondary',
                 'global.datasources.data-source-properties.ApplicationName': 'GlobalApp',
                 'global.datasources.data-source-properties.assumeMinServerVersion': '9.0']
        ))
        applicationContext.start()

        when:
        def datasourceConfigs = applicationContext.getBeansOfType(DatasourceConfiguration)

        then: "Global properties are applied to all DatasourceConfiguration beans"
        datasourceConfigs.size() == 2
        datasourceConfigs[0].connectionProperties['ApplicationName'] == 'GlobalApp'
        datasourceConfigs[0].connectionProperties['assumeMinServerVersion'] == '9.0'
        datasourceConfigs[1].connectionProperties['ApplicationName'] == 'GlobalApp'
        datasourceConfigs[1].connectionProperties['assumeMinServerVersion'] == '9.0'

        cleanup:
        applicationContext.close()
    }
}
