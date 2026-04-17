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
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.jspecify.annotations.Nullable;

import javax.sql.DataSource;

/**
 * Configures MyBatis beans from Micronaut {@link DataSource} beans.
 *
 * @author Graeme Rocher
 * @since 7.0.0
 */
@Factory
public class MyBatisFactory {

    /**
     * Creates the MyBatis {@link Configuration} for a datasource.
     *
     * @param name               The datasource name
     * @param dataSource         The datasource
     * @param transactionFactory The transaction factory, if one is provided for the datasource
     * @param applicationContext The application context
     * @return The MyBatis configuration
     */
    @EachBean(DataSource.class)
    public Configuration myBatisConfiguration(
        @Parameter String name,
        @Parameter DataSource dataSource,
        @Parameter @Nullable TransactionFactory transactionFactory,
        ApplicationContext applicationContext
    ) {
        TransactionFactory resolvedTransactionFactory = transactionFactory == null
            ? new JdbcTransactionFactory()
            : transactionFactory;
        Configuration configuration = new Configuration(new Environment(name, resolvedTransactionFactory, dataSource));
        for (MyBatisConfigurationCustomizer customizer : applicationContext.getBeansOfType(
            MyBatisConfigurationCustomizer.class,
            Qualifiers.byName(name)
        )) {
            customizer.customize(configuration);
        }
        return configuration;
    }

    /**
     * Creates the MyBatis {@link SqlSessionFactory} for a datasource.
     *
     * @param configuration The MyBatis configuration
     * @return The session factory
     */
    @EachBean(DataSource.class)
    public SqlSessionFactory sqlSessionFactory(@Parameter Configuration configuration) {
        return new SqlSessionFactoryBuilder().build(configuration);
    }

    /**
     * Creates the thread-safe {@link SqlSessionManager} for a datasource.
     *
     * @param sqlSessionFactory The session factory
     * @return The session manager
     */
    @EachBean(DataSource.class)
    public SqlSessionManager sqlSessionManager(@Parameter SqlSessionFactory sqlSessionFactory) {
        return SqlSessionManager.newInstance(sqlSessionFactory);
    }
}
