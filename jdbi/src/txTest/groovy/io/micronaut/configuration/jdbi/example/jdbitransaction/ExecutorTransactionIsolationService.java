package io.micronaut.configuration.jdbi.example.jdbitransaction;

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

    public int executeAsyncTransactionAfterCommit() throws InterruptedException {
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        transactionOperations.executeWrite(status -> {
            jdbi.useHandle(handle -> handle.execute("INSERT INTO books(id, name) VALUES(10, 'outer')"));
            status.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executorService.submit(() -> {
                        try {
                            transactionOperations.executeWrite(inner -> {
                                jdbi.useHandle(handle -> handle.execute("INSERT INTO books(id, name) VALUES(11, 'inner')"));
                                return null;
                            });
                        } catch (Throwable e) {
                            failure.set(e);
                        } finally {
                            completed.countDown();
                        }
                    });
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
        return transactionOperations.executeRead(status ->
            jdbi.withHandle(handle -> handle.createQuery("SELECT COUNT(*) FROM books").mapTo(Integer.class).one())
        );
    }
}
