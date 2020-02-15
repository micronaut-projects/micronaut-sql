/*
 * Copyright 2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.jdbi;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.reflect.ClassUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.h2.H2DatabasePlugin;
import org.jdbi.v3.core.statement.StatementBuilderFactory;
import org.jdbi.v3.core.transaction.TransactionHandler;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.util.ServiceLoader;

/**
 * Sets up Jdbi library integration.
 *
 * @author Dan Maas
 * @since 1.4.0
 */
@Factory
public class JdbiFactory {

    /**
     * Creates a Jdbi {@link Jdbi} instance.
     * It will configure it with available Jdbi provider beans with the same qualifier.
     * <p>
     * Plugins will be installed automatically from the classpath using the {@link ServiceLoader} mechanism
     *
     * @param dataSource              The {@link DataSource}
     * @param transactionHandler      The {@link TransactionHandler}
     * @param statementBuilderFactory The {@link StatementBuilderFactory}
     * @param jdbiCustomizer          The {@link JdbiCustomizer}
     * @return The {@link Jdbi} instance
     */
    @EachBean(DataSource.class)
    public Jdbi jdbi(
            DataSource dataSource,
            @Parameter @Nullable TransactionHandler transactionHandler,
            @Parameter @Nullable StatementBuilderFactory statementBuilderFactory,
            @Parameter @Nullable JdbiCustomizer jdbiCustomizer
    ) {
        Jdbi jdbi = Jdbi.create(dataSource);

        // install all plugins with ServiceLoaders that are found on the classpath
        jdbi.installPlugins();

        // transaction handler
        if (transactionHandler != null) {
            jdbi.setTransactionHandler(transactionHandler);
        }

        // statement builder
        if (statementBuilderFactory != null) {
            jdbi.setStatementBuilderFactory(statementBuilderFactory);
        }

        // customizer - allows users to create customized configurations as defined in
        // http://jdbi.org/apidocs/index.html?org/jdbi/v3/core/config/Configurable.html
        if (jdbiCustomizer != null) {
            jdbiCustomizer.customize(jdbi);
        }

        // install H2 Plugin if driver is on the classpath, as it isn't installed automatically
        if (h2IsPresent()) {
            jdbi.installPlugin(new H2DatabasePlugin());
        }

        return jdbi;
    }

    private boolean h2IsPresent() {
        return ClassUtils.isPresent("org.h2.Driver", this.getClass().getClassLoader());
    }

}
