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
package io.micronaut.configuration.jooq;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.TransactionStatus;
import io.micronaut.transaction.jdbc.DataSourceTransactionManager;
import org.jooq.TransactionContext;
import org.jooq.TransactionProvider;
import org.jooq.exception.DataAccessException;

import java.sql.Connection;

/**
 * Allows Micronaut Transaction to be used with JOOQ.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @author Vladimir Kulev
 * @since 2.0.0
 */
@Requires(classes = DataSourceTransactionManager.class)
@EachBean(DataSourceTransactionManager.class)
public class MicronautTransactionProvider implements TransactionProvider {

    private final DataSourceTransactionManager transactionManager;

    /**
     * Adapt a {@link DataSourceTransactionManager} to jOOQ transaction provider interface.
     *
     * @param transactionManager The transaction manager
     */
    public MicronautTransactionProvider(DataSourceTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void begin(TransactionContext context) throws DataAccessException {
        TransactionDefinition definition = TransactionDefinition.DEFAULT;
        TransactionStatus<Connection> status = transactionManager.getTransaction(definition);
        PropagatedContext.Scope scope = PropagatedContext.getOrEmpty()
            .plus(status.getConnectionStatus())
            .plus(status)
            .propagate();
        context.transaction(new MicronautTransaction(status, scope));
    }

    @Override
    public void commit(TransactionContext ctx) throws DataAccessException {
        MicronautTransaction transaction = getMicronautTransaction(ctx);
        try {
            transactionManager.commit(transaction.getTxStatus());
        } finally {
            transaction.getPropagatedContextScope().close();
        }
    }

    @Override
    public void rollback(TransactionContext ctx) throws DataAccessException {
        MicronautTransaction transaction = getMicronautTransaction(ctx);
        try {
            transactionManager.rollback(transaction.getTxStatus());
        } finally {
            transaction.getPropagatedContextScope().close();
        }
    }

    /**
     * Resolve the Micronaut transaction stored in jOOQ context and fail fast when
     * jOOQ transaction callbacks are invoked with an unexpected transaction object.
     *
     * @param ctx jOOQ transaction context
     * @return Micronaut transaction adapter
     * @throws DataAccessException if transaction is missing or incompatible
     */
    private MicronautTransaction getMicronautTransaction(TransactionContext ctx) {
        Object transaction = ctx.transaction();
        if (!(transaction instanceof MicronautTransaction micronautTransaction)) {
            throw new DataAccessException("Missing Micronaut transaction in jOOQ TransactionContext");
        }
        return micronautTransaction;
    }

}
