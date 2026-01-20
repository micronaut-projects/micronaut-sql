package example.jdbc.hikari.sync;

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Property;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

@Property(name = "datasources.default.dialect", value = "h2")
@Property(name = "datasources.default.url", value = "jdbc:h2:mem:devDb;LOCK_TIMEOUT=10000;DB_CLOSE_ON_EXIT=FALSE")
@Property(name = "datasources.default.username", value = "sa")
@Property(name = "datasources.default.password", value = "")
@Property(name = "datasources.default.driver-class-name", value = "org.h2.Driver")
@Property(name = "custom-ds-factory", value = "true")
@MicronautTest
class CustomDataSourceFactoryTest {

    @Inject
    ApplicationContext applicationContext;

    @Test
    void testUseCustomFactory() {
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver.class).orElse(DataSourceResolver.DEFAULT);
        Assertions.assertTrue(applicationContext.containsBean(DataSource.class));
        Assertions.assertTrue(applicationContext.containsBean(DatasourceConfiguration.class));

        DataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource.class));
        Assertions.assertTrue(dataSource instanceof CustomHikariUrlDataSource);
    }

}
