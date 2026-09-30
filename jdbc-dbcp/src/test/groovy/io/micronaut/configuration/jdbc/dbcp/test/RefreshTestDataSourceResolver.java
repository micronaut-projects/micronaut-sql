package io.micronaut.configuration.jdbc.dbcp.test;

import io.micronaut.context.annotation.Requires;
import io.micronaut.jdbc.DataSourceResolver;
import jakarta.inject.Singleton;

import javax.sql.DataSource;

@Singleton
@Requires(env = "test")
@Requires(property = "spec.name", value = "DatasourceRefreshWithWrappedDataSourceSpec")
class RefreshTestDataSourceResolver implements DataSourceResolver {

    @Override
    public DataSource resolve(DataSource dataSource) {
        if (dataSource instanceof RefreshTestDataSource wrappedDataSource) {
            return wrappedDataSource.getTargetDataSource();
        }
        return dataSource;
    }
}
