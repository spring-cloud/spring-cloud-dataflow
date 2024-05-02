/*
 * Copyright 2017-2021 the original author or authors.
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
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;

import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.ComposedTaskException;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.jdbc.support.incrementer.DataFieldMaxValueIncrementer;

/**
 * A BatchConfigurer for CTR that will establish the transaction isolation level to ISOLATION_REPEATABLE_READ by default.
 *
 * @author Glenn Renfro
 */
public class ComposedBatchConfigurer extends BasicBatchConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(ComposedBatchConfigurer.class);

	private DataSource incrementerDataSource;

	private Map<String, DataFieldMaxValueIncrementer> incrementerMap;

	private ComposedTaskProperties composedTaskProperties;

	/**
	 * Create a new {@link BasicBatchConfigurer} instance.
	 *
	 * @param properties                    the batch properties
	 * @param dataSource                    the underlying data source
	 * @param transactionManagerCustomizers transaction manager customizers (or
	 *                                      {@code null})
	 * @param composedTaskProperties composed task properties
	 */
	protected ComposedBatchConfigurer(BatchProperties properties, DataSource dataSource,
			TransactionManagerCustomizers transactionManagerCustomizers, ComposedTaskProperties composedTaskProperties) {
		super(properties, dataSource, transactionManagerCustomizers);
		this.incrementerDataSource = dataSource;
		incrementerMap = new HashMap<>();
		this.composedTaskProperties = composedTaskProperties;
	}

	protected JobRepository createJobRepository() {
		return getJobRepository();
	}

	@Override
	public JobRepository getJobRepository() {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		MultiSchemaIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(this.incrementerDataSource);
		factory.setIncrementerFactory(incrementerFactory);
		factory.setDataSource(this.incrementerDataSource);
		factory.setTransactionManager(this.getTransactionManager());
		factory.setIsolationLevelForCreate(this.composedTaskProperties.getTransactionIsolationLevel());
		try {
			factory.afterPropertiesSet();
			return factory.getObject();
		}
		catch (Exception exception) {
			throw new ComposedTaskException(exception.getMessage());
		}
	}
}
