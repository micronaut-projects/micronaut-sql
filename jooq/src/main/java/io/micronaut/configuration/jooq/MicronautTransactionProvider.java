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
 * @since 1.4.0
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
