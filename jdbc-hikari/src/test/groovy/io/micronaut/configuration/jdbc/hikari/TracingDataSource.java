package io.micronaut.configuration.jdbc.hikari;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

public class TracingDataSource implements DataSource {

    protected HikariUrlDataSource delegate;

    TracingDataSource(HikariUrlDataSource dataSource) {
        this.delegate = dataSource;
    }

    @Override
    public Connection getConnection() throws SQLException {
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        return delegate.getConnection(username, password);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return (T) this;
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return (iface.isInstance(this) || delegate.isWrapperFor(iface));
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    public String getJdbcUrl() {
        return delegate.getJdbcUrl();
    }

    public String getUsername() {
        return delegate.getUsername();
    }

    public String getPassword() {
        return delegate.getPassword();
    }

    public String getDriverClassName() {
        return delegate.getDriverClassName();
    }

    public String getConnectionTestQuery() {
        return delegate.getConnectionTestQuery();
    }
}
