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
package io.micronaut.configuration.jooq;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.r2dbc.spi.ConnectionFactory;
import org.jooq.Configuration;
import org.jooq.ConverterProvider;
import org.jooq.ExecutorProvider;
import org.jooq.MetaProvider;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordUnmapperProvider;
import org.jooq.SQLDialect;
import org.jooq.TransactionProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;

/**
 * Sets up R2DBC jOOQ library integration.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Requires(classes = ConnectionFactory.class)
@Internal
@Factory
final class R2dbcJooqConfigurationFactory extends AbstractJooqConfigurationFactory {

    /**
     * Creates jOOQ {@link Configuration}. It will configure it with available jOOQ provider beans with the same
     * qualifier.
     *
     * @param name                   The data source name
     * @param connectionFactory      The {@link ConnectionFactory}
     * @param transactionProvider    The transaction provider
     * @param settings               The settings
     * @param executorProvider       The executor provider
     * @param recordMapperProvider   The record mapper provider
     * @param recordUnmapperProvider The record unmapper provider
     * @param metaProvider           The metadata provider
     * @param converterProvider      The converter provider
     * @param properties             The properties
     * @param ctx                    The {@link ApplicationContext}
     * @return A {@link Configuration}
     */
    @SuppressWarnings("checkstyle:MethodLength")
    @EachBean(ConnectionFactory.class)
    Configuration jooqConfiguration(
        @Parameter String name,
        ConnectionFactory connectionFactory,
        @Parameter @Nullable TransactionProvider transactionProvider,
        @Parameter @Nullable Settings settings,
        @Parameter @Nullable ExecutorProvider executorProvider,
        @Parameter @Nullable RecordMapperProvider recordMapperProvider,
        @Parameter @Nullable RecordUnmapperProvider recordUnmapperProvider,
        @Parameter @Nullable MetaProvider metaProvider,
        @Parameter @Nullable ConverterProvider converterProvider,
        @Parameter @Nullable R2dbcJooqConfigurationProperties properties,
        ApplicationContext ctx) {

        if (properties == null) {
            properties = new R2dbcJooqConfigurationProperties();
        }

        DefaultConfiguration configuration = super.jooqConfiguration(name, transactionProvider, settings, executorProvider,
            recordMapperProvider, recordUnmapperProvider, metaProvider, converterProvider, properties, ctx);

        configuration.setSQLDialect(getSqlDialect(properties));
        configuration.setConnectionFactory(connectionFactory);

        return configuration;
    }

    private SQLDialect getSqlDialect(R2dbcJooqConfigurationProperties properties) {
        SQLDialect sqlDialect = properties.getSqlDialect();
        if (sqlDialect == null) {
            sqlDialect = SQLDialect.DEFAULT;
        }
        return sqlDialect;
    }
}
