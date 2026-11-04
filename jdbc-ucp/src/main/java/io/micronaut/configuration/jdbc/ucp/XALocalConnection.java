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

import javax.sql.XAConnection;
import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.ShardingKey;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

/**
 * The local connection of an XA connection borrowed from a UCP XA pool. Closing it closes the logical connection and
 * then the XA connection, which returns it to the pool. Closing the XA connection from a connection event listener
 * instead, while the logical connection is being closed, corrupts the pool with some drivers, such as H2.
 *
 * @since 7.3.0
 */
@Internal
final class XALocalConnection implements Connection {

    private final XAConnection xaConnection;
    private final Connection connection;
    private volatile boolean closed;

    /**
     * @param xaConnection The XA connection borrowed from the pool
     * @param connection   Its logical connection
     */
    XALocalConnection(XAConnection xaConnection, Connection connection) {
        this.xaConnection = xaConnection;
        this.connection = connection;
    }

    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            connection.close();
        } catch (SQLException | RuntimeException e) {
            try {
                xaConnection.close();
            } catch (SQLException | RuntimeException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
        xaConnection.close();
    }

    @Override
    public boolean isClosed() throws SQLException {
        return closed || connection.isClosed();
    }

    @Override
    public void abort(Executor arg0) throws SQLException {
        connection.abort(arg0);
    }

    @Override
    public void beginRequest() throws SQLException {
        connection.beginRequest();
    }

    @Override
    public void clearWarnings() throws SQLException {
        connection.clearWarnings();
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public Array createArrayOf(String arg0, Object[] arg1) throws SQLException {
        return connection.createArrayOf(arg0, arg1);
    }

    @Override
    public Blob createBlob() throws SQLException {
        return connection.createBlob();
    }

    @Override
    public Clob createClob() throws SQLException {
        return connection.createClob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        return connection.createNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        return connection.createSQLXML();
    }

    @Override
    public Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    @Override
    public Statement createStatement(int arg0, int arg1) throws SQLException {
        return connection.createStatement(arg0, arg1);
    }

    @Override
    public Statement createStatement(int arg0, int arg1, int arg2) throws SQLException {
        return connection.createStatement(arg0, arg1, arg2);
    }

    @Override
    public Struct createStruct(String arg0, Object[] arg1) throws SQLException {
        return connection.createStruct(arg0, arg1);
    }

    @Override
    public void endRequest() throws SQLException {
        connection.endRequest();
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    @Override
    public String getCatalog() throws SQLException {
        return connection.getCatalog();
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        return connection.getClientInfo();
    }

    @Override
    public String getClientInfo(String arg0) throws SQLException {
        return connection.getClientInfo(arg0);
    }

    @Override
    public int getHoldability() throws SQLException {
        return connection.getHoldability();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return connection.getMetaData();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        return connection.getNetworkTimeout();
    }

    @Override
    public String getSchema() throws SQLException {
        return connection.getSchema();
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return connection.getTransactionIsolation();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return connection.getTypeMap();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        return connection.getWarnings();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return connection.isReadOnly();
    }

    @Override
    public boolean isValid(int arg0) throws SQLException {
        return connection.isValid(arg0);
    }

    @Override
    public boolean isWrapperFor(Class<?> arg0) throws SQLException {
        return arg0.isInstance(this) || connection.isWrapperFor(arg0);
    }

    @Override
    public String nativeSQL(String arg0) throws SQLException {
        return connection.nativeSQL(arg0);
    }

    @Override
    public CallableStatement prepareCall(String arg0) throws SQLException {
        return connection.prepareCall(arg0);
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2) throws SQLException {
        return connection.prepareCall(arg0, arg1, arg2);
    }

    @Override
    public CallableStatement prepareCall(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        return connection.prepareCall(arg0, arg1, arg2, arg3);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0) throws SQLException {
        return connection.prepareStatement(arg0);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int[] arg1) throws SQLException {
        return connection.prepareStatement(arg0, arg1);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, String[] arg1) throws SQLException {
        return connection.prepareStatement(arg0, arg1);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1) throws SQLException {
        return connection.prepareStatement(arg0, arg1);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2) throws SQLException {
        return connection.prepareStatement(arg0, arg1, arg2);
    }

    @Override
    public PreparedStatement prepareStatement(String arg0, int arg1, int arg2, int arg3) throws SQLException {
        return connection.prepareStatement(arg0, arg1, arg2, arg3);
    }

    @Override
    public void releaseSavepoint(Savepoint arg0) throws SQLException {
        connection.releaseSavepoint(arg0);
    }

    @Override
    public void rollback() throws SQLException {
        connection.rollback();
    }

    @Override
    public void rollback(Savepoint arg0) throws SQLException {
        connection.rollback(arg0);
    }

    @Override
    public void setAutoCommit(boolean arg0) throws SQLException {
        connection.setAutoCommit(arg0);
    }

    @Override
    public void setCatalog(String arg0) throws SQLException {
        connection.setCatalog(arg0);
    }

    @Override
    public void setClientInfo(Properties arg0) throws SQLClientInfoException {
        connection.setClientInfo(arg0);
    }

    @Override
    public void setClientInfo(String arg0, String arg1) throws SQLClientInfoException {
        connection.setClientInfo(arg0, arg1);
    }

    @Override
    public void setHoldability(int arg0) throws SQLException {
        connection.setHoldability(arg0);
    }

    @Override
    public void setNetworkTimeout(Executor arg0, int arg1) throws SQLException {
        connection.setNetworkTimeout(arg0, arg1);
    }

    @Override
    public void setReadOnly(boolean arg0) throws SQLException {
        connection.setReadOnly(arg0);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return connection.setSavepoint();
    }

    @Override
    public Savepoint setSavepoint(String arg0) throws SQLException {
        return connection.setSavepoint(arg0);
    }

    @Override
    public void setSchema(String arg0) throws SQLException {
        connection.setSchema(arg0);
    }

    @Override
    public void setShardingKey(ShardingKey arg0) throws SQLException {
        connection.setShardingKey(arg0);
    }

    @Override
    public void setShardingKey(ShardingKey arg0, ShardingKey arg1) throws SQLException {
        connection.setShardingKey(arg0, arg1);
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey arg0, int arg1) throws SQLException {
        return connection.setShardingKeyIfValid(arg0, arg1);
    }

    @Override
    public boolean setShardingKeyIfValid(ShardingKey arg0, ShardingKey arg1, int arg2) throws SQLException {
        return connection.setShardingKeyIfValid(arg0, arg1, arg2);
    }

    @Override
    public void setTransactionIsolation(int arg0) throws SQLException {
        connection.setTransactionIsolation(arg0);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
        connection.setTypeMap(arg0);
    }

    @Override
    public <T> T unwrap(Class<T> arg0) throws SQLException {
        return arg0.isInstance(this) ? arg0.cast(this) : connection.unwrap(arg0);
    }
}
