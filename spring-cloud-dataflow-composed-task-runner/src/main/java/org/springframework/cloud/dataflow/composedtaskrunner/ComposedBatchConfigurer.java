/*
 * Copyright 2017-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.composedtaskrunner;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.batch.BasicBatchConfigurer;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.transaction.annotation.Isolation;

/**
 * A BatchConfigurer for CTR that will establish the transaction isolation lavel to READ_COMMITTED.
 *
 * @author Glenn Renfro
 */
public class ComposedBatchConfigurer extends BasicBatchConfigurer {
	/**
	 * Create a new {@link BasicBatchConfigurer} instance.
	 *
	 * @param properties                    the batch properties
	 * @param dataSource                    the underlying data source
	 * @param transactionManagerCustomizers transaction manager customizers (or
	 *                                      {@code null})
	 */
	protected ComposedBatchConfigurer(BatchProperties properties, DataSource dataSource, TransactionManagerCustomizers transactionManagerCustomizers) {
		super(properties, dataSource, transactionManagerCustomizers);
	}

	@Override
	protected String determineIsolationLevel() {
		return "ISOLATION_" + Isolation.READ_COMMITTED;
	}
}
