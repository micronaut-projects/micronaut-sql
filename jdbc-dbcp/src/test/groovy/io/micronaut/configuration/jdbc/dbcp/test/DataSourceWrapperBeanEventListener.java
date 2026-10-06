package io.micronaut.configuration.jdbc.dbcp.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;

import javax.sql.DataSource;

@Singleton
@Requires(env = "test")
@Requires(property = "spec.name", value = "DatasourceRefreshWithWrappedDataSourceSpec")
public class DataSourceWrapperBeanEventListener implements BeanCreatedEventListener<DataSource>, Ordered {

    @Override
    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        return new RefreshTestDataSource(event.getBean());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
