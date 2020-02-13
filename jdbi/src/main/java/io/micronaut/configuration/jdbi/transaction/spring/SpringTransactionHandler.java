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

package io.micronaut.configuration.jdbi.transaction.spring;

import io.micronaut.configuration.jdbi.transaction.AbstractTransactionHandler;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Requires;
import org.jdbi.v3.core.Handle;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Allows Spring Transaction to be used with Jdbi.
 *
 * @author Dan Maas
 * @since 1.3.1
 */
@Requires(classes = PlatformTransactionManager.class)
@EachBean(PlatformTransactionManager.class)
public class SpringTransactionHandler extends AbstractTransactionHandler {

	private final ConcurrentHashMap<Handle, LocalStuff> localStuff = new ConcurrentHashMap<>();

    private final PlatformTransactionManager transactionManager;

    /**
     * Adapt a {@link PlatformTransactionManager} to Jdbi transaction provider interface.
     *
     * @param transactionManager The transaction manager
     */
    public SpringTransactionHandler(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Override
    public void begin(Handle handle) {
        TransactionDefinition definition = new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_NESTED);
        TransactionStatus status = this.transactionManager.getTransaction(definition);
        this.localStuff.putIfAbsent(handle, new LocalStuff(status));
    }

    @Override
    public void commit(Handle handle) {
        withLocalStuff(handle, (localStuff) -> {
			try {
				this.transactionManager.commit(localStuff.getTransactionStatus());
			} finally {
				restore(handle);
			}
        });
    }

    @Override
    public void rollback(Handle handle) {
		didTxnRollback.set(true);
		withLocalStuff(handle, (localStuff) -> {
			try {
				this.transactionManager.rollback(localStuff.getTransactionStatus());
			} finally {
				restore(handle);
			}
		});
    }

    @Override
    public boolean isInTransaction(Handle handle) {
    	TransactionStatus status = getTransactionStatus(handle);
        return status != null && !status.isCompleted();
    }

    @Override
    public void savepoint(Handle handle, String savepointName) {
		withLocalStuff(handle, (localStuff) -> {
			Object savePoint = localStuff.getTransactionStatus().createSavepoint();
			localStuff.getSavepoints().put(savepointName, savePoint);
		});
    }

    @Override
    public void rollbackToSavepoint(Handle handle, String savepointName) {
		withLocalStuff(handle, (localStuff) -> {
			Object savePoint = localStuff.getSavepoints().get(savepointName);
			if (savePoint != null) {
				localStuff.getTransactionStatus().rollbackToSavepoint(savePoint);
			}
		});
    }

    @Override
    public void releaseSavepoint(Handle handle, String savepointName) {
		withLocalStuff(handle, (localStuff) -> {
			Object savePoint = localStuff.getSavepoints().remove(savepointName);
			if (savePoint != null) {
				localStuff.getTransactionStatus().releaseSavepoint(savePoint);
			}
		});
    }

    private TransactionStatus getTransactionStatus(Handle handle) {
        LocalStuff localStuff = this.localStuff.get(handle);
        return localStuff != null ? localStuff.getTransactionStatus() : null;
    }

	private void withLocalStuff(Handle handle, Consumer<LocalStuff> consumer) {
		LocalStuff localStuff = this.localStuff.get(handle);
		if (localStuff != null) {
			consumer.accept(localStuff);
		}
	}

	private void restore(final Handle handle) {
		try {
			final LocalStuff stuff = this.localStuff.remove(handle);
			if (stuff != null) {
				stuff.getSavepoints().clear();
			}
		} finally {
			// prevent memory leak if rollback throws an exception
			this.localStuff.remove(handle);
		}
	}

	private static class LocalStuff {

		private final Map<String, Object> savepoints = new HashMap<>();

		private final TransactionStatus transactionStatus;

		LocalStuff(TransactionStatus transactionStatus) {
			this.transactionStatus = transactionStatus;
		}

		Map<String, Object> getSavepoints() {
			return savepoints;
		}

		TransactionStatus getTransactionStatus() {
			return transactionStatus;
		}

	}

}
