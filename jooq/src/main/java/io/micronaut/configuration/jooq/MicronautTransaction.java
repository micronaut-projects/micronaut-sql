/*
 * Copyright 2017-2020 original authors
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

import io.micronaut.transaction.TransactionStatus;
import org.jooq.Transaction;

import java.sql.Connection;

/**
 * Adapts a Micronaut transaction for JOOQ.
 *
 * @author Lukas Eder
 * @author Andreas Ahlenstorf
 * @author Phillip Webb
 * @author Vladimir Kulev
 * @since 2.0.0
 */
class MicronautTransaction implements Transaction {

	private final TransactionStatus<Connection> transactionStatus;

	/**
	 * Wrap existing {@link TransactionStatus} object with jOOQ transaction.
	 *
	 * @param transactionStatus The transaction status object
	 */
	MicronautTransaction(TransactionStatus<Connection> transactionStatus) {
		this.transactionStatus = transactionStatus;
	}

	/**
	 * Get underlying transaction status.
	 *
	 * @return The transaction status object
	 */
	public TransactionStatus<Connection> getTxStatus() {
		return this.transactionStatus;
	}

}
