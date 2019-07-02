/*
 * Copyright 2017-2018 original authors
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

package io.micronaut.configuration.jooq.spring;

import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Requires;
import org.jooq.TransactionContext;
import org.jooq.TransactionProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Allows Spring Transaction to be used with JOOQ.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @since 1.2.0
 */
@Requires(classes = PlatformTransactionManager.class)
@EachBean(PlatformTransactionManager.class)
public class SpringTransactionProvider implements TransactionProvider {

	// Based on the jOOQ-spring-example from https://github.com/jOOQ/jOOQ

	private final PlatformTransactionManager transactionManager;

	/**
	 * Adapt a {@link PlatformTransactionManager} to jOOQ transaction provider interface.
	 *
	 * @param transactionManager The transaction manager
	 */
	public SpringTransactionProvider(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void begin(TransactionContext context) {
		TransactionDefinition definition = new DefaultTransactionDefinition(
				TransactionDefinition.PROPAGATION_NESTED);
		TransactionStatus status = this.transactionManager.getTransaction(definition);
		context.transaction(new SpringTransaction(status));
	}

	@Override
	public void commit(TransactionContext ctx) {
		this.transactionManager.commit(getTransactionStatus(ctx));
	}

	@Override
	public void rollback(TransactionContext ctx) {
		this.transactionManager.rollback(getTransactionStatus(ctx));
	}

	private TransactionStatus getTransactionStatus(TransactionContext ctx) {
		SpringTransaction transaction = (SpringTransaction) ctx.transaction();
		return transaction.getTxStatus();
	}

}
