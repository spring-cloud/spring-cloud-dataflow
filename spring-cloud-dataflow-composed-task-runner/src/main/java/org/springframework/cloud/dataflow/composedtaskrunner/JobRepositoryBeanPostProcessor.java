/*
 * Copyright 2024 the original author or authors.
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.ComposedTaskException;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaIncrementerFactory;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * CTR requires that the JobRepository that it uses to have its own {@link MultiSchemaIncrementerFactory}.
 * As of Batch 5.x DefaultBatchConfiguration is now used to override default beans, however this disables
 * BatchAutoConfiguration.  To work around this we use a bean post processor to create our own {@link JobRepository}.
 *
 * @author Glenn Renfro
 */
public class JobRepositoryBeanPostProcessor implements BeanPostProcessor, Ordered {
	private static final Logger logger = LoggerFactory.getLogger(JobRepositoryBeanPostProcessor.class);

	private PlatformTransactionManager transactionManager;
	private DataSource incrementerDataSource;
	private ComposedTaskProperties composedTaskProperties;

	public JobRepositoryBeanPostProcessor(PlatformTransactionManager transactionManager, DataSource incrementerDataSource,
										  ComposedTaskProperties composedTaskProperties) {
		this.transactionManager = transactionManager;
		this.incrementerDataSource = incrementerDataSource;
		this.composedTaskProperties = composedTaskProperties;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (beanName.equals("jobRepository")) {
			logger.debug("Replacing BatchAutoConfiguration's jobRepository Bean with one provided by composed task runner.");
			bean = jobRepository(transactionManager, incrementerDataSource, composedTaskProperties);
		}
		return bean;
	}

	private JobRepository jobRepository(PlatformTransactionManager transactionManager, DataSource incrementerDataSource,
										ComposedTaskProperties composedTaskProperties) {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		MultiSchemaIncrementerFactory incrementerFactory = new MultiSchemaIncrementerFactory(incrementerDataSource);
		factory.setIncrementerFactory(incrementerFactory);
		factory.setDataSource(incrementerDataSource);
		factory.setTransactionManager(transactionManager);
		factory.setIsolationLevelForCreate(composedTaskProperties.getTransactionIsolationLevel());
		try {
			factory.afterPropertiesSet();
			return factory.getObject();
		}
		catch (Exception exception) {
			throw new ComposedTaskException(exception.getMessage());
		}
	}
}
