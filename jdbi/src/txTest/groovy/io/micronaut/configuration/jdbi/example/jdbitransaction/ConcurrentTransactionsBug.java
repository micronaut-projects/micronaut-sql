package io.micronaut.configuration.jdbi.example.jdbitransaction;

import io.micronaut.configuration.jdbi.example.dao.BookMapper;
import io.micronaut.configuration.jdbi.example.dao.BooksDao;
import io.micronaut.configuration.jdbi.example.model.Book;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jdbi.v3.core.Handle;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

@Singleton
public class ConcurrentTransactionsBug {
    final BooksDao nonTransactional;

    final BooksDao withTransaction;
    Semaphore semMain = new Semaphore(0);
    Semaphore semThief = new Semaphore(0);
    Semaphore semDone = new Semaphore(0);
    @Inject
    public ConcurrentTransactionsBug(
        @Named("nonTransactional") BooksDao booksDao,
        @Named("transactions") BooksDao withTransaction) {
        this.nonTransactional = booksDao;
        this.withTransaction = withTransaction;
    }

    public void stealingThread() {
        // once we are in our Connection context, release main thread to being a parallel transaction
        BookMapper.toRelease.set(semMain);
        // our Mapper will wait until semThief is released - that will be done by BookMapper of the main thread
        BookMapper.locker.set(semThief);
        // do some transactonal query
        try {
            nonTransactional.listNoTransaction();
            // release the other thread working with a transaction
        } catch (Exception e) {
            LoggerFactory.getLogger(ConcurrentTransactionsBug.class).error("Error", e);
        } finally {
            // our job completed (hopefully with exception), release the main thread
            semDone.release(100);
        }
    }

    /**
     * Demonstrates how one thread steals a transaction from another thread.
     * This is caused by a buggy lookup in {@link io.micronaut.configuration.jdbi.transaction.micronaut.MicronautDataTransactionHandler#getTransactionStatus}
     * Since it uses Handle as a key for Lookup. Handles for the same Jdbi injectable Bean use the
     * SAME instance of ContextualConnection and its {@link Handle#equals(Object)} compares the Jdbi
     * (is the same) and Connection (is the SAME instance) for all Handles created by Jdbi instance.
     * <p>
     * Therefore, queries like isInTransaction() and other operations of MicronautDataTransactionHandler
     * are likely to mix threads together.
     * <p>
     * Timing is critical. A transactionless query must start before a concurrent transaction
     * on the same Connection starts, and must end before that concurrent transaction commits.
     * To do so, we use Semaphores to establish that timing.
     * The offspring thread executes first, and its RowMapper releases this main thread to
     * create the transanction. The main thread stops and is released only after the
     *
     * @throws Exception
     */
    public void transactionStealedFromOtherThread() throws Exception {
        CompletableFuture<?> f = CompletableFuture.runAsync(this::stealingThread);
        // wait until the transactionless thread is in the middle of its work
        semMain.acquire();

        BookMapper.toRelease.set(semThief);
        BookMapper.locker.set(semDone);

        List<Book> books = withTransaction.listAll();
        f.get();
    }

    /**
     * Will throw io.micronaut.data.connection.exceptions.NoConnectionException
     * The reason is that in {@link io.micronaut.data.connection.support.AbstractConnectionOperations}
     * {@link io.micronaut.data.connection.support.AbstractConnectionOperations#suspendOpenConnection}
     * FIRST modifies {@link io.micronaut.core.propagation.PropagatedContext} so it ("A") one lists NO
     * connection, then during newSupplier.get(), that restore of that reduced context "A" is pushed into
     * connection's synchronizations by the newSupplier.
     * AFTER the supplied returns, the Scope "B" that could restore suspended connection is added to
     * synchronizations; it will be ordered AFTER "A", and therefore rolled back first on connection
     * complete. So instead restoring PropagatedContext in the order "A", "B", the states will be
     * restored in the order "B", "A", leaving incorrect state after closing the connection returned
     * by suspendOpenConnection().
     * <p>
     * In addition, if obtaining a new connection fails (i.e. pool exhausted etc), suspendOpenConnection
     * FAILS to restore the previous context, as Scope is not rolled back in finally, it is not added
     * to connection synchronizations.
     */
    public void connectionStatusLostDuringExecution() {
        nonTransactional.listAll();
    }
}
