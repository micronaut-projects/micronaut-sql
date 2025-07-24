package example.jdbc.hikari.sync;

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.DefaultApplicationContext;
import io.micronaut.jdbc.DataSourceResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

class CustomDataSourceFactoryTest {

    @Test
    void testUseCustomFactory() {
        ApplicationContext applicationContext = new DefaultApplicationContext("test");
        applicationContext.start();
        DataSourceResolver dataSourceResolver = applicationContext.findBean(DataSourceResolver.class).orElse(DataSourceResolver.DEFAULT);
        Assertions.assertTrue(applicationContext.containsBean(DataSource.class));
        Assertions.assertTrue(applicationContext.containsBean(DatasourceConfiguration.class));

        DataSource dataSource = dataSourceResolver.resolve(applicationContext.getBean(DataSource.class));
        Assertions.assertTrue(dataSource instanceof CustomHikariUrlDataSource);
        applicationContext.stop();
    }

}
