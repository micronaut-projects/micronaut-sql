/*
 * Copyright 2017-2022 original authors
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
package io.micronaut.configuration.jooq

import io.micronaut.context.ApplicationContext
import io.micronaut.context.annotation.Factory
import io.micronaut.context.annotation.Requires
import io.r2dbc.spi.ConnectionFactory
import jakarta.inject.Named
import jakarta.inject.Singleton
import org.jooq.DSLContext
import org.jooq.SQLDialect
import spock.lang.Specification

import javax.sql.DataSource
import java.lang.reflect.Proxy

class DslContextFactorySpec extends Specification {

    void "test default dsl context can be injected with jdbc and r2dbc configurations"() {
        given:
        ApplicationContext applicationContext = ApplicationContext.run("multiple-config")

        when:
        TestService testService = applicationContext.getBean(TestService)

        then:
        testService.dslContext != null
        testService.dslContext.configuration() instanceof R2dbcConfiguration

        cleanup:
        applicationContext.close()
    }

    @Singleton
    @Requires(env = "multiple-config")
    static class TestService {
        final DSLContext dslContext

        TestService(DSLContext dslContext) {
            this.dslContext = dslContext
        }
    }

    @Factory
    @Requires(env = "multiple-config")
    static class TestConnectionFactory {

        @Singleton
        @Named("default")
        DataSource dataSource() {
            return proxy(DataSource)
        }

        @Singleton
        @Named("default")
        ConnectionFactory connectionFactory() {
            return proxy(ConnectionFactory)
        }

        @Singleton
        @Named("analytics")
        ConnectionFactory analyticsConnectionFactory() {
            return proxy(ConnectionFactory)
        }

        @Singleton
        @Named("default")
        JooqConfigurationProperties jdbcJooqProperties() {
            JooqConfigurationProperties configurationProperties = new JooqConfigurationProperties()
            configurationProperties.sqlDialect = SQLDialect.H2
            return configurationProperties
        }

        @Singleton
        @Named("default")
        R2dbcJooqConfigurationProperties r2dbcJooqProperties() {
            R2dbcJooqConfigurationProperties configurationProperties = new R2dbcJooqConfigurationProperties()
            configurationProperties.sqlDialect = SQLDialect.POSTGRES
            return configurationProperties
        }

        @Singleton
        @Named("analytics")
        R2dbcJooqConfigurationProperties analyticsR2dbcJooqProperties() {
            R2dbcJooqConfigurationProperties configurationProperties = new R2dbcJooqConfigurationProperties()
            configurationProperties.sqlDialect = SQLDialect.MYSQL
            return configurationProperties
        }

        private static <T> T proxy(Class<T> type) {
            return (T) Proxy.newProxyInstance(type.classLoader, [type] as Class<?>[]) { _, method, _ ->
                if (method.name == "toString") {
                    return type.simpleName
                }
                if (method.returnType == Boolean.TYPE) {
                    return false
                }
                if (method.returnType == Integer.TYPE) {
                    return 0
                }
                return null
            }
        }
    }
}
