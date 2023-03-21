package example.hibernate6.reactive;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.transaction.TransactionDefinition;
import io.micronaut.transaction.reactive.ReactiveTransactionOperations;
import io.micronaut.transaction.reactive.ReactiveTransactionStatus;
import org.hibernate.SessionFactory;
import org.hibernate.reactive.stage.Stage;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;

@EachBean(SessionFactory.class)
public class TxManager implements ReactiveTransactionOperations<Stage.Session> {

    public static final String SESSION_KEY = "HibernateReactiveSession";
    private final Stage.SessionFactory sessionFactory;

    public TxManager(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory.unwrap(Stage.SessionFactory.class);
    }

    @Override
    public <T> Publisher<T> withTransaction(TransactionDefinition definition, TransactionalCallback<Stage.Session, T> handler) {
        return Mono.fromCompletionStage(() -> sessionFactory.withTransaction((session, transaction) -> {
            try {
                return Mono.from(handler.doInTransaction(new ReactiveTransactionStatus<>() {
                        @Override
                        public Stage.Session getConnection() {
                            return session;
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
                    })).contextWrite(context -> context.put(SESSION_KEY, session))
                    .toFuture();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
    }
}
