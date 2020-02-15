/*
 * Copyright 2020 original authors
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

package io.micronaut.configuration.jdbi.transaction;

import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.HandleCallback;
import org.jdbi.v3.core.transaction.TransactionHandler;
import org.jdbi.v3.core.transaction.TransactionIsolationLevel;

/**
 * Base class for using transaction management with Jdbi.
 *
 * @author Dan Maas
 * @since 1.4.0
 */
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
