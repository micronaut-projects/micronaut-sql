package example.jdbc.hikari.sync;

import io.micronaut.configuration.jdbc.hikari.DatasourceConfiguration;
import io.micronaut.configuration.jdbc.hikari.DatasourceFactory;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Replaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

@Factory
@Replaces(DatasourceFactory.class)
public class CustomDataSourceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(CustomDataSourceFactory.class);

    @Context
    @EachBean(DatasourceConfiguration.class)
    @Replaces(bean = DataSource.class, factory = DatasourceFactory.class)
    public DataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        LOG.info("Created custom datasource");
        return new CustomHikariUrlDataSource(datasourceConfiguration);
    }
}

