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
package io.micronaut.configuration.mybatis;

import io.micronaut.context.ApplicationContext;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.configuration.mybatis.support.TestTransactionFactory;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyBatisFactoryTest {

    @Test
    void doesNotCreateBeansWithoutDataSource() {
        try (ApplicationContext applicationContext = ApplicationContext.builder("test").start()) {
            assertFalse(applicationContext.containsBean(DataSource.class));
            assertFalse(applicationContext.containsBean(Configuration.class));
            assertFalse(applicationContext.containsBean(SqlSessionFactory.class));
            assertFalse(applicationContext.containsBean(SqlSessionManager.class));
        }
    }

    @Test
    void createsMyBatisBeansForTheDefaultDataSource() throws Exception {
        try (ApplicationContext applicationContext = ApplicationContext.builder("test")
            .properties(Map.of("datasources.default", Map.of()))
            .start()) {
            assertTrue(applicationContext.containsBean(DataSource.class));
            assertTrue(applicationContext.containsBean(Configuration.class));
            assertTrue(applicationContext.containsBean(SqlSessionFactory.class));
            assertTrue(applicationContext.containsBean(SqlSessionManager.class));

            Configuration configuration = applicationContext.getBean(Configuration.class);
            assertTrue(configuration.hasMapper(TestMapper.class));
            assertTrue(configuration.isMapUnderscoreToCamelCase());
            assertInstanceOf(TestTransactionFactory.class, configuration.getEnvironment().getTransactionFactory());
            assertSame(
                applicationContext.getBean(TransactionFactory.class, Qualifiers.byName("default")),
                configuration.getEnvironment().getTransactionFactory()
            );

            SqlSessionFactory sqlSessionFactory = applicationContext.getBean(SqlSessionFactory.class);
            assertNotNull(sqlSessionFactory.getConfiguration().getEnvironment());
            assertEquals("default", sqlSessionFactory.getConfiguration().getEnvironment().getId());

            initializeSchema(applicationContext.getBean(DataSource.class));

            SqlSessionManager sqlSessionManager = applicationContext.getBean(SqlSessionManager.class);
            TestMapper mapper = sqlSessionManager.getMapper(TestMapper.class);
            assertEquals(1, mapper.count());
            mapper.insert();
            assertEquals(2, mapper.count());
        }
    }

    @Test
    void ignoresWronglyQualifiedBeansAndFallsBackToJdbcTransactions() {
        try (ApplicationContext applicationContext = ApplicationContext.builder("test")
            .properties(Map.of("datasources.default2", Map.of()))
            .start()) {
            assertTrue(applicationContext.containsBean(DataSource.class));
            assertTrue(applicationContext.containsBean(Configuration.class));
            assertTrue(applicationContext.containsBean(SqlSessionFactory.class));
            assertTrue(applicationContext.containsBean(SqlSessionManager.class));

            Configuration configuration = applicationContext.getBean(Configuration.class);
            assertFalse(configuration.hasMapper(TestMapper.class));
            assertInstanceOf(JdbcTransactionFactory.class, configuration.getEnvironment().getTransactionFactory());
        }
    }

    private static void initializeSchema(DataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE foo(id INT PRIMARY KEY)");
            statement.execute("INSERT INTO foo(id) VALUES(1)");
        }
    }
}
