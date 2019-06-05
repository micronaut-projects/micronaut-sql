package io.micronaut.configuration.jooq;

import io.micronaut.context.annotation.EachProperty;
import org.jooq.SQLDialect;
import org.jooq.tools.jdbc.JDBCUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@EachProperty(value = "jooq")
public class JooqConfigurationProperties {

    private SQLDialect sqlDialect;

    public SQLDialect getSqlDialect() {
        return sqlDialect;
    }

    public void setSqlDialect(SQLDialect sqlDialect) {
        this.sqlDialect = sqlDialect;
    }

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
