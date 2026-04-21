/*
 * Copyright 2017-2022 original authors
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

import io.micronaut.context.BeanProvider;
import io.micronaut.context.annotation.Any;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.annotation.Secondary;
import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.qualifiers.Qualifiers;
import io.micronaut.transaction.TransactionStatus;
import io.micronaut.transaction.support.ExceptionUtil;
import jakarta.inject.Singleton;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.TransactionProperty;
import org.jooq.TransactionalCallable;
import org.jooq.TransactionalRunnable;
import org.jooq.impl.DefaultDSLContext;

/**
 * Builds {@link DSLContext}.
 *
 * @author Denis Stepanov
 * @since 4.5.0
 */
@Factory
@Internal
final class DSLContextFactory {

    /**
     * Created {@link DSLContext} based on {@link Configuration}.
     *
     * @param configuration The {@link Configuration}
     * @return A {@link DSLContext}
     */
    @EachBean(JdbcConfiguration.class)
    @Secondary
    DSLContext dslContext(@Parameter JdbcConfiguration configuration) {
        return createDslContext(configuration);
    }

    @EachBean(R2dbcConfiguration.class)
    @Secondary
    DSLContext r2dbcDslContext(@Parameter R2dbcConfiguration configuration) {
        return createDslContext(configuration);
    }

    @Primary
    @Singleton
    @Requires(beans = R2dbcConfiguration.class)
    DSLContext primaryR2dbcDslContext(@Any BeanProvider<R2dbcConfiguration> configurations) {
        R2dbcConfiguration configuration = selectR2dbcConfiguration(configurations);
        return createDslContext(configuration);
    }

    private R2dbcConfiguration selectR2dbcConfiguration(BeanProvider<R2dbcConfiguration> configurations) {
        return configurations.find(Qualifiers.byStereotype(Primary.class))
            .or(() -> configurations.find(Qualifiers.byName("default")))
            .orElseGet(() -> requireSingleConfiguration(configurations));
    }

    private R2dbcConfiguration requireSingleConfiguration(BeanProvider<R2dbcConfiguration> configurations) {
        var iterator = configurations.iterator();
        var configuration = iterator.next();
        if (iterator.hasNext()) {
            int candidateCount = 2;
            while (iterator.hasNext()) {
                iterator.next();
                candidateCount++;
            }
            throw new IllegalStateException("Multiple R2DBC configurations found (" + candidateCount + "). Mark one @Primary or name one 'default'");
        }
        return configuration;
    }

    private DSLContext createDslContext(Configuration configuration) {
        return new DefaultDSLContext(configuration) {

            @Override
            public <T> T transactionResult(TransactionalCallable<T> transactional) {
                return super.transactionResult(propagateTransaction(transactional));
            }

            @Override
            public <T> T transactionResult(TransactionalCallable<T> transactional, TransactionProperty... properties) {
                return super.transactionResult(propagateTransaction(transactional), properties);
            }

            @Override
            public void transaction(TransactionalRunnable transactional) {
                super.transaction(propagateTransaction(transactional));
            }

            @Override
            public void transaction(TransactionalRunnable transactional, TransactionProperty... properties) {
                super.transaction(propagateTransaction(transactional), properties);
            }

            private <T> TransactionalCallable<T> propagateTransaction(TransactionalCallable<T> transactional) {
                return new TransactionalCallable<>() {
                    @Override
                    public T run(Configuration configuration) throws Throwable {
                        TransactionStatus<?> transactionStatus = (TransactionStatus<?>) configuration.data().get(MicronautTransactionProvider.TX_KEY);
                        if (transactionStatus != null) {
                            return transactionStatus.propagate(() -> {
                                try {
                                    return transactional.run(configuration);
                                } catch (Throwable e) {
                                    return ExceptionUtil.sneakyThrow(e);
                                }
                            });
                        }
                        return transactional.run(configuration);
                    }
                };
            }

            private TransactionalRunnable propagateTransaction(TransactionalRunnable transactional) {
                return new TransactionalRunnable() {
                    @Override
                    public void run(Configuration configuration) throws Throwable {
                        propagateTransaction((TransactionalCallable<Void>) innerConfiguration -> {
                            transactional.run(innerConfiguration);
                            return null;
                        }).run(configuration);
                    }
                };
            }
        };
    }

}
