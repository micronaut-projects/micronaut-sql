/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.micronaut.configuration.jooq;

import io.micronaut.context.annotation.EachProperty;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.JDBCUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Configuration for jOOQ.
 *
 * @author Vladimir Kulev
 * @since 1.2.0
 */
@EachProperty(value = "jooq")
public class JooqConfigurationProperties {

    private SQLDialect sqlDialect;

    /**
     * SQL dialect to use. If {@code null}, will be detected automatically.
     *
     * @return SQL dialect
     */
    public SQLDialect getSqlDialect() {
        return sqlDialect;
    }

    /**
     * SQL dialect to use. Will be detected automatically by default.
     *
     * @param sqlDialect SQL dialect
     */
    public void setSqlDialect(SQLDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

    /**
     * Resolve {@link SQLDialect} to be used for the data source.
     * If SQL dialect is not set explicitly, automatic detection will be done.
     *
     * @param dataSource data source for automatic detection
     * @return Effective SQL dialect
     */
    public SQLDialect determineSqlDialect(DataSource dataSource) {
        if (this.sqlDialect != null) {
            return this.sqlDialect;
        }
        if (dataSource == null) {
            return SQLDialect.DEFAULT;
        }
        try (Connection connection = dataSource.getConnection()) {
            return JDBCUtils.dialect(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
