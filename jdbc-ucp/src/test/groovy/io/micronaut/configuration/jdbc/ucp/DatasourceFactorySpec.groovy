/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.configuration.jdbc.ucp

import io.micronaut.context.ApplicationContext
import io.micronaut.context.event.BeanDestroyedEvent
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.qualifiers.Qualifiers
import io.micronaut.jdbc.DataSourceResolver
import oracle.ucp.admin.UniversalConnectionPoolManager
import oracle.ucp.jdbc.PoolDataSource
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceFactorySpec extends Specification {

    void "destroys the pool of a data source once its bean is destroyed"() {
        given:
        ApplicationContext applicationContext = startContext('destroyedPool')
        DatasourceFactory factory = applicationContext.getBean(DatasourceFactory)
        UniversalConnectionPoolManager manager = applicationContext.getBean(UniversalConnectionPoolManager)
        String poolName = openPool(applicationContext)

        when: "a data source bean this factory did not produce is destroyed"
        factory.onDestroyed(new BeanDestroyedEvent(applicationContext, foreignDefinition(), Mock(DataSource)))

        then:
        manager.connectionPoolNames.contains(poolName)

        when:
        applicationContext.stop()

        then:
        !manager.connectionPoolNames.contains(poolName)
    }

    void "close() destroys the pools that remain"() {
        given:
        ApplicationContext applicationContext = startContext('remainingPool')
        UniversalConnectionPoolManager manager = applicationContext.getBean(UniversalConnectionPoolManager)
        String poolName = openPool(applicationContext)

        when:
        applicationContext.getBean(DatasourceFactory).close()

        then:
        !manager.connectionPoolNames.contains(poolName)

        cleanup:
        applicationContext.close()
    }

    private static ApplicationContext startContext(String database) {
        return ApplicationContext.run([
                "datasources.default.url"     : "jdbc:h2:mem:${database};DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE".toString(),
                "datasources.default.username": "sa",
                "datasources.default.password": ""
        ], "test")
    }

    private static String openPool(ApplicationContext applicationContext) {
        DataSourceResolver resolver = applicationContext.findBean(DataSourceResolver).orElse(DataSourceResolver.DEFAULT)
        PoolDataSource pool = (PoolDataSource) resolver.resolve(applicationContext.getBean(DataSource))
        pool.getConnection().close()
        return pool.connectionPoolName
    }

    private BeanDefinition<DataSource> foreignDefinition() {
        BeanDefinition<DataSource> definition = Mock()
        definition.getDeclaringType() >> Optional.of(String)
        definition.getDeclaredQualifier() >> Qualifiers.byName('default')
        return definition
    }
}
