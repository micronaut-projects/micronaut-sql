package io.micronaut.configuration.jdbc.ucp.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.event.BeanCreatedEvent;
import io.micronaut.context.event.BeanCreatedEventListener;
import io.micronaut.core.order.Ordered;
import jakarta.inject.Singleton;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

/**
 * A {@link BeanCreatedEventListener} that wraps each {@link DataSource} in a {@link TransactionAwareDataSourceProxy}
 * to create an example of a wrapped {@link DataSource} for test cases.
 */
@Singleton
@Requires(env = "test")
public class DataSourceWrapperBeanEventListener implements BeanCreatedEventListener<DataSource>, Ordered {

    public DataSource onCreated(BeanCreatedEvent<DataSource> event) {
        final DataSource dataSource = event.getBean();

        return new TransactionAwareDataSourceProxy(dataSource);
    }

    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

}
