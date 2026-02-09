package io.micronaut.configuration.jdbc.ucp.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.jdbc.DataSourceResolver;
import jakarta.inject.Singleton;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

import javax.sql.DataSource;

/**
 * A {@link DataSourceResolver} that unwraps a {@link DataSource} from a {@link TransactionAwareDataSourceProxy}
 * as an example of a wrapped {@link DataSource} for test cases.
 */
@Singleton
@Requires(env = "test")
class TransactionAwareDataSourceProxyResolver implements DataSourceResolver {

    @Override
    public DataSource resolve(DataSource dataSource) {
        if (dataSource instanceof TransactionAwareDataSourceProxy transactionAwareDataSourceProxy) {
            return transactionAwareDataSourceProxy.getTargetDataSource();
        }

        return dataSource;
    }
}
