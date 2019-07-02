/*
 * Copyright 2017-2018 original authors
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

package io.micronaut.configuration.jooq;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.inject.qualifiers.Qualifiers;
import org.jooq.*;
import org.jooq.conf.Settings;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultDSLContext;

import javax.annotation.Nullable;
import javax.sql.DataSource;

/**
 * Sets up jOOQ library integration.
 *
 * @author Vladimir Kulev
 * @since 1.2.0
 */
@Factory
public class JooqConfigurationFactory {

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
     * @param ctx                    The {@link ApplicationContext}
     * @return A {@link Configuration}
     */
    @EachBean(DataSource.class)
    public Configuration jooqConfiguration(
            @Parameter String name,
            DataSource dataSource,
            @Parameter @Nullable TransactionProvider transactionProvider,
            @Parameter @Nullable Settings settings,
            @Parameter @Nullable ExecutorProvider executorProvider,
            @Parameter @Nullable RecordMapperProvider recordMapperProvider,
            @Parameter @Nullable RecordUnmapperProvider recordUnmapperProvider,
            @Parameter @Nullable MetaProvider metaProvider,
            ApplicationContext ctx
    ) {
        DefaultConfiguration configuration = new DefaultConfiguration();

        JooqConfigurationProperties properties = ctx.findBean(JooqConfigurationProperties.class, Qualifiers.byName(name))
                .orElseGet(JooqConfigurationProperties::new);
        configuration.setSQLDialect(properties.determineSqlDialect(dataSource));

        configuration.setDataSource(dataSource);
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

    /**
     * Created {@link DSLContext} based on {@link Configuration}
     *
     * @param configuration The {@link Configuration}
     * @return A {@link DSLContext}
     */
    @EachBean(Configuration.class)
    public DSLContext dslContext(Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }

}
