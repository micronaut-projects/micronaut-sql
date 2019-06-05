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

@Factory
public class JooqConfigurationFactory {

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
        configuration.setExecuteListenerProvider(ctx.getBeansOfType(ExecuteListenerProvider.class, Qualifiers.byName(name))
                .toArray(new ExecuteListenerProvider[0]));
        configuration.setRecordListenerProvider(ctx.getBeansOfType(RecordListenerProvider.class, Qualifiers.byName(name))
                .toArray(new RecordListenerProvider[0]));
        configuration.setVisitListenerProvider(ctx.getBeansOfType(VisitListenerProvider.class, Qualifiers.byName(name))
                .toArray(new VisitListenerProvider[0]));
        configuration.setTransactionListenerProvider(ctx.getBeansOfType(TransactionListenerProvider.class, Qualifiers.byName(name))
                .toArray(new TransactionListenerProvider[0]));

        return configuration;
    }

    @EachBean(Configuration.class)
    public DSLContext dslContext(Configuration configuration) {
        return new DefaultDSLContext(configuration);
    }

}
