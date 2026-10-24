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
package io.micronaut.configuration.jdbc.ucp;

import io.micronaut.core.annotation.Internal;
import oracle.ucp.jdbc.PoolXADataSource;

import javax.sql.XAConnection;
import javax.sql.XAConnectionBuilder;
import javax.sql.XADataSource;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * The {@link XADataSource} bean of a UCP XA pool. It delegates to the {@link PoolXADataSource} and, unlike the pool,
 * is not a {@link javax.sql.DataSource}, so the listeners and proxies of {@link javax.sql.DataSource} beans do not
 * apply to it a second time.
 *
 * @param pool the XA pool
 * @since 7.3.0
 */
@Internal
record UcpXADataSource(PoolXADataSource pool) implements XADataSource {

    @Override
    public XAConnection getXAConnection() throws SQLException {
        return pool.getXAConnection();
    }

    @Override
    public XAConnection getXAConnection(String user, String password) throws SQLException {
        return pool.getXAConnection(user, password);
    }

    @Override
    public XAConnectionBuilder createXAConnectionBuilder() throws SQLException {
        return ((XADataSource) pool).createXAConnectionBuilder();
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return pool.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        pool.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        pool.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return pool.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return pool.getParentLogger();
    }
}
