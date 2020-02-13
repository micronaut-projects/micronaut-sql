package io.micronaut.configuration.jdbi.transaction;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

public abstract class AbstractTransactionHandler implements TransactionHandler {

    protected final ThreadLocal<Boolean> didTxnRollback = ThreadLocal.withInitial(() -> false);

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle, HandleCallback<R, X> callback) throws X {
        if (isInTransaction(handle)) {
            throw new IllegalStateException("Already in transaction");
        }
        didTxnRollback.set(false);
        final R returnValue;
        try {
            handle.begin();
            returnValue = callback.withHandle(handle);
            if (!didTxnRollback.get()) {
                handle.commit();
            }
        } catch (Throwable e) {
            try {
                handle.rollback();
            } catch (Exception rollback) {
                e.addSuppressed(rollback);
            }
            throw e;
        }

        didTxnRollback.remove();
        return returnValue;
    }

    @Override
    public <R, X extends Exception> R inTransaction(Handle handle,
                                                    TransactionIsolationLevel level,
                                                    HandleCallback<R, X> callback) throws X {
        final TransactionIsolationLevel initial = handle.getTransactionIsolationLevel();
        try {
            handle.setTransactionIsolation(level);
            return inTransaction(handle, callback);
        } finally {
            handle.setTransactionIsolation(initial);
        }
    }

}
