package io.micronaut.configuration.jdbc.hikari.custom;

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration;
import io.micronaut.configuration.jdbc.hikari.DatasourceFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

@Factory
@Replaces(factory = DatasourceFactory.class)
@Requires(property = "use-custom-factory", value = "true")
class CustomDataSourceFactory extends DatasourceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(CustomDataSourceFactory.class);

    CustomDataSourceFactory(ApplicationContext applicationContext) {
        super(applicationContext);
        LOG.info("CustomDataSourceFactory created");
    }

    @Override
    @Context
    @EachBean(DatasourceConfiguration.class)
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        LOG.info("Creating CustomDataSource");
        return new CustomDataSource(datasourceConfiguration);
    }
}
