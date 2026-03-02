package example.hibernate.reactive;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.core.async.propagation.ReactorPropagation;
import io.micronaut.data.connection.ConnectionDefinition;
import io.micronaut.data.connection.ConnectionStatus;
import io.micronaut.data.connection.DefaultConnectionDefinition;
import io.micronaut.data.connection.reactive.DefaultReactiveConnectionStatus;
import io.micronaut.data.connection.reactive.ReactorConnectionOperations;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.reactive.ReactiveTransactionOperations;
import io.micronaut.transaction.reactive.ReactiveTransactionStatus;
import org.jspecify.annotations.NonNull;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.stage.Stage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;

import java.util.Optional;

@EachBean(SessionFactory.class)
public class TxManager implements ReactiveTransactionOperations<Stage.Session> {

    public static final String SESSION_KEY = "HibernateReactiveSession";

    private final ReactorConnectionOperations<Stage.Session> connectionOperations = new ReactorConnectionOperations<>() {
        @Override
        public Optional<ConnectionStatus<Stage.Session>> findConnectionStatus(@NonNull ContextView contextView) {
            return ReactorPropagation.findAllContextElements(contextView, ConnectionStatus.class)
                    .filter(e -> managesConnection(e))
                    .map(status -> (ConnectionStatus<Stage.Session>) status)
                    .findFirst();
        }

        @Override
        public boolean managesConnection(@NonNull ConnectionStatus<Stage.Session> connectionStatus) {
            if (!(connectionStatus instanceof DefaultReactiveConnectionStatus<?> defaultReactiveConnectionStatus)) {
                return false;
            }
            return isManagedByThis(defaultReactiveConnectionStatus);
        }

        @SuppressWarnings("unchecked")
        private boolean isManagedByThis(DefaultReactiveConnectionStatus<?> connectionStatus) {
            return ((DefaultReactiveConnectionStatus<Stage.Session>) connectionStatus).isConnectionOf(this);
        }
    };

    private final Stage.SessionFactory sessionFactory;

    public TxManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory.unwrap(Stage.SessionFactory.class);
    }

    @Override
    public <T> Publisher<T> withTransaction(TransactionDefinition definition,
                                            TransactionalCallback<Stage.Session, T> handler) {
        return Mono.fromCompletionStage(() -> sessionFactory.withTransaction((session, transaction) -> {
            try {
                return Mono.from(handler.doInTransaction(new ReactiveTransactionStatus<>() {
                    @Override
                    public Stage.Session getConnection() {
                        return session;
                    }

                    @Override
                    public @NonNull ConnectionStatus<Stage.Session> getConnectionStatus() {
                        return new DefaultReactiveConnectionStatus<>(
                            session,
                            new DefaultConnectionDefinition(ConnectionDefinition.Propagation.REQUIRED),
                            connectionOperations,
                            true
                        );
                    }

                    @Override
                    public boolean isNewTransaction() {
                        return true;
                    }

                    @Override
                    public void setRollbackOnly() {
                        transaction.markForRollback();
                    }

                    @Override
                    public boolean isRollbackOnly() {
                        return transaction.isMarkedForRollback();
                    }

                    @Override
                    public boolean isCompleted() {
                        return false;
                    }

                    @Override
                    public @NonNull TransactionDefinition getTransactionDefinition() {
                        return definition;
                    }
                })).contextWrite(context -> context.put(SESSION_KEY, session)).toFuture();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }

    @Override
    public boolean managesTransaction(@NonNull ReactiveTransactionStatus<Stage.Session> transactionStatus) {
        return connectionOperations.managesConnection(transactionStatus.getConnectionStatus());
    }
}
