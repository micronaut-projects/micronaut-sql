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
import io.micronaut.context.annotation.Parameter;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.jooq.ConverterProvider;
import org.jooq.DiagnosticsListenerProvider;
import org.jooq.ExecuteListenerProvider;
import org.jooq.ExecutorProvider;
import org.jooq.MetaProvider;
import org.jooq.RecordListenerProvider;
import org.jooq.RecordMapperProvider;
import org.jooq.RecordUnmapperProvider;
import org.jooq.TransactionListenerProvider;
import org.jooq.TransactionProvider;
import org.jooq.VisitListenerProvider;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;

/**
 * Sets up jOOQ library integration.
 *
 * @author Vladimir Kulev
 * @since 1.2.0
 */
@Internal
abstract class AbstractJooqConfigurationFactory {

    /**
     * Creates jOOQ {@link org.jooq.Configuration}.
     * It will configure it with available jOOQ provider beans with the same qualifier.
     *
     * @param name                   The data source name
     * @param transactionProvider    The transaction provider
     * @param settings               The settings
     * @param executorProvider       The executor provider
     * @param recordMapperProvider   The record mapper provider
     * @param recordUnmapperProvider The record unmapper provider
     * @param metaProvider           The metadata provider
     * @param converterProvider      The converter provider
     * @param properties             The properties
     * @param ctx                    The {@link ApplicationContext}
     * @return A {@link org.jooq.Configuration}
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    protected DefaultConfiguration jooqConfiguration(
        @Parameter String name,
        @Parameter @Nullable TransactionProvider transactionProvider,
        @Parameter @Nullable Settings settings,
        @Parameter @Nullable ExecutorProvider executorProvider,
        @Parameter @Nullable RecordMapperProvider recordMapperProvider,
        @Parameter @Nullable RecordUnmapperProvider recordUnmapperProvider,
        @Parameter @Nullable MetaProvider metaProvider,
        @Parameter @Nullable ConverterProvider converterProvider,
        AbstractJooqConfigurationProperties properties,
        ApplicationContext ctx
    ) {
        DefaultConfiguration configuration = new DefaultConfiguration();

        if (transactionProvider != null) {
            configuration.setTransactionProvider(transactionProvider);
        }
        if (settings != null) {
            configuration.setSettings(settings);
        }
        if (executorProvider != null) {
            configuration.setExecutorProvider(executorProvider);
        }
        if (recordMapperProvider != null) {
            configuration.setRecordMapperProvider(recordMapperProvider);
        }
        if (recordUnmapperProvider != null) {
            configuration.setRecordUnmapperProvider(recordUnmapperProvider);
        }
        if (metaProvider != null) {
            configuration.setMetaProvider(metaProvider);
        }
        if (converterProvider != null) {
            configuration.set(converterProvider);
        } else if (properties.isJsonConverterEnabled()) {
            ctx.findBean(JsonConverterProvider.class).ifPresent(configuration::set);
        }
        configuration.setExecuteListenerProvider(ctx.getBeansOfType(ExecuteListenerProvider.class, Qualifiers.byName(name))
            .toArray(new ExecuteListenerProvider[0]));
        configuration.setRecordListenerProvider(ctx.getBeansOfType(RecordListenerProvider.class, Qualifiers.byName(name))
            .toArray(new RecordListenerProvider[0]));
        configuration.setVisitListenerProvider(ctx.getBeansOfType(VisitListenerProvider.class, Qualifiers.byName(name))
            .toArray(new VisitListenerProvider[0]));
        configuration.setTransactionListenerProvider(ctx.getBeansOfType(TransactionListenerProvider.class, Qualifiers.byName(name))
            .toArray(new TransactionListenerProvider[0]));
        configuration.setDiagnosticsListenerProvider(ctx.getBeansOfType(DiagnosticsListenerProvider.class, Qualifiers.byName(name))
            .toArray(new DiagnosticsListenerProvider[0]));

        return configuration;
    }

}
