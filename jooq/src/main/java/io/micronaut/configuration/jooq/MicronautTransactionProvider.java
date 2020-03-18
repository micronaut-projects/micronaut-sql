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
        context.transaction(new MicronautTransaction(status));
    }

    @Override
    public void commit(TransactionContext ctx) throws DataAccessException {
        transactionManager.commit(getTransactionStatus(ctx));
    }

    @Override
    public void rollback(TransactionContext ctx) throws DataAccessException {
        transactionManager.rollback(getTransactionStatus(ctx));
    }

    private TransactionStatus<Connection> getTransactionStatus(TransactionContext ctx) {
        MicronautTransaction transaction = (MicronautTransaction) ctx.transaction();
        return transaction.getTxStatus();
    }

}
