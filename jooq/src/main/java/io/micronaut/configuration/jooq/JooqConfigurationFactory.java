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
     * @param name       The data source name
     * @param dataSource The {@link DataSource}
     * @param ctx        The {@link ApplicationContext}
     * @return A {@link Configuration}
     */
    @EachBean(DataSource.class)
    public Configuration jooqConfiguration(
            @Parameter String name,
            DataSource dataSource,
            ApplicationContext ctx
    ) {
        DefaultConfiguration configuration = new DefaultConfiguration();

        JooqConfigurationProperties properties = ctx.findBean(JooqConfigurationProperties.class, Qualifiers.byName(name))
                .orElseGet(JooqConfigurationProperties::new);
        configuration.setSQLDialect(properties.determineSqlDialect(dataSource));

        configuration.setDataSource(dataSource);
        ctx.findBean(TransactionProvider.class, Qualifiers.byName(name)).ifPresent(configuration::setTransactionProvider);
        ctx.findBean(Settings.class, Qualifiers.byName(name)).ifPresent(configuration::setSettings);
        ctx.findBean(ExecutorProvider.class, Qualifiers.byName(name)).ifPresent(configuration::setExecutorProvider);
        ctx.findBean(RecordMapperProvider.class, Qualifiers.byName(name)).ifPresent(configuration::setRecordMapperProvider);
        ctx.findBean(RecordUnmapperProvider.class, Qualifiers.byName(name)).ifPresent(configuration::setRecordUnmapperProvider);
        ctx.findBean(MetaProvider.class, Qualifiers.byName(name)).ifPresent(configuration::setMetaProvider);
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
