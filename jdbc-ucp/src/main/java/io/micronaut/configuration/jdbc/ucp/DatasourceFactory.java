package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Factory
public class DatasourceFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);
    private List<PoolDataSource> dataSources = new ArrayList<>(2);

    private ApplicationContext applicationContext;

    /**
     * Default constructor.
     *
     * @param applicationContext The application context
     */
    public DatasourceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    /**
     * Method to get a PoolDataSource from the {@link DatasourceConfiguration}.
     *
     * @param datasourceConfiguration A {@link DatasourceConfiguration}
     * @return A {@link PoolDataSource}
     */
    @Context
    @EachBean(DatasourceConfiguration.class)
    public PoolDataSource dataSource(DatasourceConfiguration datasourceConfiguration) {
        PoolDataSource ds = datasourceConfiguration.delegate;
        dataSources.add(ds);
        return ds;
    }

    @Override
    @PreDestroy
    public void close() {
        for (PoolDataSource dataSource : dataSources) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Closing connection pool named: {}", dataSource.getConnectionPoolName());
                }
                UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager().destroyConnectionPool(dataSource.getConnectionPoolName());
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Error closing data source [" + dataSource + "]: " + e.getMessage(), e);
                }
            }
        }
    }
}