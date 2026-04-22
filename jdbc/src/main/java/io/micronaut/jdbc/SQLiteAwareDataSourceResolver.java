/*
 * Copyright 2017-2026 original authors
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
package io.micronaut.jdbc;

import io.micronaut.core.annotation.Internal;
import jakarta.inject.Singleton;

import javax.sql.DataSource;

/**
 * Resolves wrapped SQLite-aware data sources to their underlying target datasource.
 *
 * @since 7.0.0
 */
@Singleton
@Internal
public final class SQLiteAwareDataSourceResolver implements DataSourceResolver {

    @Override
    public DataSource resolve(DataSource dataSource) {
        if (dataSource instanceof JdbcSqliteSupport.SQLiteAwareDataSource sqliteAwareDataSource) {
            return sqliteAwareDataSource.getTargetDataSource();
        }
        return dataSource;
    }
}
