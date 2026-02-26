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
package io.micronaut.configuration.jdbi.transaction.micronaut;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Requires;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.impl.DefaultTransactionStatus;
import io.micronaut.transaction.jdbc.DataSourceTransactionManager;
import io.micronaut.transaction.support.DefaultTransactionDefinition;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * Allows Micronaut data Transaction to be used with Jdbi.
 *
 * @author Dan Maas
 * @since 1.4.0
 */
@Requires(classes = DataSourceTransactionManager.class)
@EachBean(DataSourceTransactionManager.class)
public class MicronautDataTransactionHandler implements TransactionHandler {

    private final DataSourceTransactionManager transactionManager;

    /**
     * Adapt a {@link DataSourceTransactionManager} to Jdbi transaction provider interface.
     *
     * @param transactionManager The transaction manager
     */
    public MicronautDataTransactionHandler(DataSourceTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void begin(Handle handle) {
        transactionManager.getTransaction(TransactionDefinition.DEFAULT);
    }

    @Override
    public void commit(Handle handle) {
        transactionManager.commit(getRequiredTxStatus());
    }

    @Override
    public void rollback(Handle handle) {
        transactionManager.rollback(getRequiredTxStatus());
    }

    @Override
    public boolean isInTransaction(Handle handle) {
        return transactionManager.findTransactionStatus().isPresent();
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle, HandleCallback<R, X> callback) {
        return transactionManager.execute(TransactionDefinition.DEFAULT, status -> callback.withHandle(handle));
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle, TransactionIsolationLevel level, HandleCallback<R, X> callback) {
        DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition(TransactionDefinition.Propagation.REQUIRED);
        transactionDefinition.setIsolationLevel(switch (level) {
            case NONE, UNKNOWN -> TransactionDefinition.Isolation.DEFAULT;
            case READ_UNCOMMITTED -> TransactionDefinition.Isolation.READ_UNCOMMITTED;
            case READ_COMMITTED -> TransactionDefinition.Isolation.READ_COMMITTED;
            case REPEATABLE_READ -> TransactionDefinition.Isolation.REPEATABLE_READ;
            case SERIALIZABLE -> TransactionDefinition.Isolation.SERIALIZABLE;
        });
        return transactionManager.execute(transactionDefinition, status -> callback.withHandle(handle));
    }

    @Override
    public void savepoint(Handle handle, String savepointName) {
        try {
            DefaultTransactionStatus<Connection> requiredTxStatus = getRequiredTxStatus();
            Savepoint savepoint = requiredTxStatus.getConnectionStatus().getConnection().setSavepoint(savepointName);
            requiredTxStatus.setSavepoint(savepoint);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void rollbackToSavepoint(Handle handle, String savepointName) {
        try {
            DefaultTransactionStatus<Connection> requiredTxStatus = getRequiredTxStatus();
            requiredTxStatus.getConnectionStatus().getConnection().rollback((Savepoint) requiredTxStatus.getSavepoint());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void releaseSavepoint(Handle handle, String savepointName) {
        try {
            DefaultTransactionStatus<Connection> requiredTxStatus = getRequiredTxStatus();
            requiredTxStatus.getConnectionStatus().getConnection().releaseSavepoint((Savepoint) requiredTxStatus.getSavepoint());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private DefaultTransactionStatus<Connection> getRequiredTxStatus() {
        return transactionManager.findTransactionStatusInternal()
            .orElseThrow(() -> new IllegalStateException("No transaction status found"));
    }

}
