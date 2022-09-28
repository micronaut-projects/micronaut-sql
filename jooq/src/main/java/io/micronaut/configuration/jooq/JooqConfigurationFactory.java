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
package io.micronaut.configuration.jooq;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.jdbc.DataSourceResolver;
import jdk.jshell.spi.ExecutionControl;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.ConverterProvider;
import org.jooq.DSLContext;
import org.jooq.ExecutorProvider;
import org.jooq.MetaProvider;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordUnmapperProvider;
import org.jooq.TransactionProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;

import javax.sql.DataSource;

/**
 * Sets up jOOQ library integration.
 *
 * @author Vladimir Kulev
 * @since 1.2.0
 */
@Factory
public class JooqConfigurationFactory extends AbstractJooqConfigurationFactory {

    /**
     * Creates jOOQ {@link Configuration}.
     * It will configure it with available jOOQ provider beans with the same qualifier.
     *
     * @param name                   The data source name
     * @param dataSource             The {@link DataSource}
     * @param transactionProvider    The transaction provider
     * @param settings               The settings
     * @param executorProvider       The executor provider
     * @param recordMapperProvider   The record mapper provider
     * @param recordUnmapperProvider The record unmapper provider
     * @param metaProvider           The metadata provider
     * @param converterProvider      The converter provider
     * @param connectionProvider     The connection provider
     * @param properties             The properties
     * @param dataSourceResolver     The dataSourceResolver
     * @param ctx                    The context
     * @return A {@link Configuration}
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    @EachBean(DataSource.class)
    Configuration jooqConfiguration(
        @Parameter String name,
        DataSource dataSource,
        @Parameter @Nullable TransactionProvider transactionProvider,
        @Parameter @Nullable Settings settings,
        @Parameter @Nullable ExecutorProvider executorProvider,
        @Parameter @Nullable RecordMapperProvider recordMapperProvider,
        @Parameter @Nullable RecordUnmapperProvider recordUnmapperProvider,
        @Parameter @Nullable MetaProvider metaProvider,
        @Parameter @Nullable ConverterProvider converterProvider,
        @Parameter @Nullable ConnectionProvider connectionProvider,
        @Parameter @Nullable JooqConfigurationProperties properties,
        @Nullable DataSourceResolver dataSourceResolver,
        @Nullable ApplicationContext ctx
    ) {

        if (properties == null) {
            properties = new JooqConfigurationProperties();
        }
        if (dataSourceResolver == null) {
            dataSourceResolver = DataSourceResolver.DEFAULT;
        }

        DefaultConfiguration configuration = super.jooqConfiguration(name, transactionProvider, settings, executorProvider,
            recordMapperProvider, recordUnmapperProvider, metaProvider, converterProvider, properties, ctx);

        if (connectionProvider != null) {
            configuration.setConnectionProvider(connectionProvider);
        }

        configuration.setSQLDialect(properties.determineSqlDialect(dataSourceResolver.resolve(dataSource)));

        if (transactionProvider != null) {
            configuration.setTransactionProvider(transactionProvider);
        }
        if (connectionProvider == null) {
            configuration.setDataSource(dataSource);
        }

        return configuration;
    }

    /**
     * Created {@link DSLContext} based on {@link Configuration}.
     *
     * @param configuration The {@link Configuration}
     * @return A {@link DSLContext}
     * @deprecated Handled via the {@link DSLContextFactory} instead.
     */
    @Deprecated
    public DSLContext dslContext(Configuration configuration) {
        throw new UnsupportedOperationException("Moved to DSLContextFactory");
    }

}
