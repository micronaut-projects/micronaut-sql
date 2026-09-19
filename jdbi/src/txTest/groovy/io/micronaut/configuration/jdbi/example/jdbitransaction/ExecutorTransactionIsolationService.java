package io.micronaut.configuration.jdbi.example.jdbitransaction;

import io.micronaut.core.propagation.PropagatedContext;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.transaction.TransactionOperations;
import io.micronaut.transaction.support.TransactionSynchronization;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Jdbi;

import java.sql.Connection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class ExecutorTransactionIsolationService {

    private final Jdbi jdbi;
    private final TransactionOperations<Connection> transactionOperations;
    private final ExecutorService executorService;

    public ExecutorTransactionIsolationService(
        Jdbi jdbi,
        @Named("default") TransactionOperations<Connection> transactionOperations,
        @Named(TaskExecutors.SCHEDULED) ExecutorService executorService
    ) {
        this.jdbi = jdbi;
        this.transactionOperations = transactionOperations;
        this.executorService = executorService;
    }

    public ExecutionResult executeAsyncTransactionAfterCommit() throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicBoolean innerNewTransaction = new AtomicBoolean(false);

        transactionOperations.executeWrite(status -> {
            int outerBookId = nextBookId();
            jdbi.useHandle(handle -> handle.execute("INSERT INTO books(id, name) VALUES(?, ?)", outerBookId, "outer"));
            status.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        PropagatedContext contextWithoutTransaction = PropagatedContext.getOrEmpty()
                            .minus(status)
                            .minus(status.getConnectionStatus());
                        executorService.submit(contextWithoutTransaction.wrap(() -> {
                            try {
                                transactionOperations.executeWrite(inner -> {
                                    innerNewTransaction.set(inner.isNewTransaction());
                                    int innerBookId = nextBookId();
                                    jdbi.useHandle(handle -> handle.execute("INSERT INTO books(id, name) VALUES(?, ?)", innerBookId, "inner"));
                                    return null;
                                });
                            } catch (Throwable e) {
                                failure.set(e);
                            } finally {
                                completed.countDown();
                            }
                        }));
                    } catch (Throwable e) {
                        failure.set(e);
                        completed.countDown();
                    }
                }
            });
            return null;
        });

        if (!completed.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for asynchronous transactional work");
        }
        Throwable throwable = failure.get();
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable != null) {
            throw new RuntimeException(throwable);
        }
        int count = transactionOperations.executeRead(status ->
            jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM books").mapTo(Integer.class).one())
        );
        return new ExecutionResult(count, innerNewTransaction.get());
    }

    private int nextBookId() {
        return jdbi.withHandle(handle ->
            handle.createQuery("SELECT COALESCE(MAX(id), 0) + 1 FROM books")
                .mapTo(Integer.class)
                .one()
        );
    }

    public record ExecutionResult(int count, boolean innerNewTransaction) {
    }
}
