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
package io.micronaut.configuration.jdbc.dbcp

import io.micronaut.jdbc.DataSourceResolver
import org.apache.commons.dbcp2.BasicDataSource
import spock.lang.Specification

import javax.sql.DataSource

class DatasourceFactorySpec extends Specification {

    def "create basic datasource"() {
        given:
        def dataSource = new BasicDataSource(validationQuery: "SELECT 1")
        DatasourceFactory datasourceFactory = new DatasourceFactory(new DataSourceResolver() {
            @Override
            DataSource resolve(DataSource ds) {
                return ds
            }
        })
        when:
        def metadata = datasourceFactory.dbcpDataSourcePoolMetadata(dataSource)

        then:
        metadata
        metadata.idle >= 0
        metadata.max >= 0
        metadata.active >= 0
        metadata.getValidationQuery()
        metadata.usage >= 0
    }

    def "create proxy datasource"() {
        given:
        def dataSource = new BasicDataSource(validationQuery: "SELECT 1")
        def proxyDataSource = Spy(dataSource)
        def dataSourceResolver = new DataSourceResolver() {
            @Override
            DataSource resolve(DataSource ds) {
                if (ds.is(proxyDataSource)) {
                    return dataSource
                }
                return ds
            }
        }
        DatasourceFactory datasourceFactory = new DatasourceFactory(dataSourceResolver)

        when:
        def metadata = datasourceFactory.dbcpDataSourcePoolMetadata(proxyDataSource)

        then:
        metadata
        metadata.idle >= 0
        metadata.max >= 0
        metadata.active >= 0
        metadata.validationQuery == "SELECT 1"
        metadata.usage >= 0
    }
}
