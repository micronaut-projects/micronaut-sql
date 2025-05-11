/*
 * Copyright 2017-2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.configuration.jdbc.dbcp;

import io.micronaut.configuration.jdbc.dbcp.metadata.DbcpDataSourcePoolMetadata;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.jdbc.DataSourcePasswordChangedEvent;
import io.micronaut.jdbc.DataSourceResolver;
import io.micronaut.jdbc.metadata.DataSourcePoolMetadata;
import org.apache.commons.dbcp2.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Creates a dbcp data source for each configuration bean.
 *
 * @author Christian Oestreich
 * @since 1.0
 */
@Factory
public class DatasourceFactory implements ApplicationEventListener<DataSourcePasswordChangedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(DatasourceFactory.class);

    private final DataSourceResolver dataSourceResolver;
    private final ApplicationContext applicationContext;

    /**
     * Default constructor.
     * @param dataSourceResolver The data source resolver
     * @param applicationContext The application context
     */
    public DatasourceFactory(@Nullable DataSourceResolver dataSourceResolver,
                             ApplicationContext applicationContext) {
        this.dataSourceResolver = dataSourceResolver == null ? DataSourceResolver.DEFAULT : dataSourceResolver;
        this.applicationContext = applicationContext;
    }

    /**
     * Method to create a metadata object that allows pool value lookup for each datasource object.
     *
     * @param dataSource The actual datasource
     * @return a {@link io.micronaut.jdbc.metadata.DataSourcePoolMetadata}
     */
    @EachBean(DataSource.class)
    public DataSourcePoolMetadata<BasicDataSource> dbcpDataSourcePoolMetadata(
            DataSource dataSource) {
        DbcpDataSourcePoolMetadata dbcpDataSourcePoolMetadata = null;
        DataSource resolved = dataSourceResolver.resolve(dataSource);

        if (resolved instanceof BasicDataSource basicDataSource) {
            dbcpDataSourcePoolMetadata = new DbcpDataSourcePoolMetadata(basicDataSource);
        }
        return dbcpDataSourcePoolMetadata;
    }

    @Override
    public void onApplicationEvent(DataSourcePasswordChangedEvent event) {
        DataSourcePasswordChangedEvent.DataSourcePasswordModel dataSourcePasswordModel = event.getDataSourcePasswordModel();
        String dataSourceName = dataSourcePasswordModel.dataSourceName();
        Optional<DataSource> optionalDataSource = applicationContext.findBean(DataSource.class, Qualifiers.byName(dataSourceName));
        if (!optionalDataSource.isPresent()) {
            return;
        }
        DataSource dataSource = dataSourceResolver.resolve(optionalDataSource.get());
        if (dataSource instanceof BasicDataSource basicDataSource) {
            basicDataSource.setPassword(dataSourcePasswordModel.newPassword());
            try {
                basicDataSource.restart();
            } catch (SQLException e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to restart datasource after password change {}", dataSourceName, e);
                }
            }
        }
    }
}
