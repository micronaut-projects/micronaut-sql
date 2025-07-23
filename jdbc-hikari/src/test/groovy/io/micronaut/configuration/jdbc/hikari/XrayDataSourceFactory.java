package io.micronaut.configuration.jdbc.hikari;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;

import javax.sql.DataSource;

@Factory
@Replaces(factory = DatasourceFactory.class)
@Requires(property = "use-custom-factory", value = "true")
class XrayDataSourceFactory extends DatasourceFactory {
    XrayDataSourceFactory(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    @Context
    @EachBean(DatasourceConfiguration.class)
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        return new TracingDataSource((HikariUrlDataSource) super.dataSource(datasourceConfiguration));
    }
}
