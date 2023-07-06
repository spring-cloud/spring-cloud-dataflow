/*
 * Copyright 2017-2023 the original author or authors.
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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.core.database.support.MultiSchemaTaskExecutionDaoFactoryBean;
import org.springframework.cloud.dataflow.core.dsl.TaskApp;
import org.springframework.cloud.dataflow.core.dsl.TaskParser;
import org.springframework.cloud.task.configuration.EnableTask;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configures the Job that will execute the Composed Task Execution.
 *
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@EnableBatchProcessing
@EnableTask
@EnableConfigurationProperties(ComposedTaskProperties.class)
@Configuration
@Import(org.springframework.cloud.dataflow.composedtaskrunner.StepBeanDefinitionRegistrar.class)
public class ComposedTaskRunnerConfiguration {
	private final static Logger logger = LoggerFactory.getLogger(ComposedTaskRunnerConfiguration.class);

	@Bean
	public StepExecutionListener composedTaskStepExecutionListener(TaskExplorerContainer taskExplorerContainer) {
		return new org.springframework.cloud.dataflow.composedtaskrunner.ComposedTaskStepExecutionListener(taskExplorerContainer);
	}

	@Bean
	TaskExplorerContainer taskExplorerContainer(TaskExplorer taskExplorer, DataSource dataSource, ComposedTaskProperties properties, Environment env) {
		Map<String, TaskExplorer> explorers = new HashMap<>();
		String ctrName = env.getProperty("spring.cloud.task.name");
		if(ctrName == null) {
			throw  new IllegalStateException("spring.cloud.task.name property must have a value.");
		}
		TaskParser parser = new TaskParser("ctr", properties.getGraph(), false, true);
		StepBeanDefinitionRegistrar.TaskAppsMapCollector collector = new StepBeanDefinitionRegistrar.TaskAppsMapCollector();
		parser.parse().accept(collector);
		Set<String> taskNames = collector.getTaskApps().keySet();
		for (String taskName : taskNames) {

			String propertyName = String.format("app.%s.spring.cloud.task.tablePrefix", taskName);
			String prefix = properties.getComposedTaskAppProperties().get(propertyName);
			if(prefix == null) {
				prefix = env.getProperty(propertyName);
			}
			if (prefix != null) {
				TaskExecutionDaoFactoryBean factoryBean = new MultiSchemaTaskExecutionDaoFactoryBean(dataSource, prefix);
				explorers.put(taskName, new SimpleTaskExplorer(factoryBean));
			} else {
				logger.warn("Cannot find {} in {} ", propertyName, properties.getComposedTaskAppProperties());
			}
			String appName = taskName.replace(ctrName + "-", "");
			propertyName = String.format("app.%s.spring.cloud.task.tablePrefix", appName);
			prefix = properties.getComposedTaskAppProperties().get(propertyName);
			if(prefix == null) {
				prefix = env.getProperty(propertyName);
			}
			if (prefix != null) {
				TaskExecutionDaoFactoryBean factoryBean = new MultiSchemaTaskExecutionDaoFactoryBean(dataSource, prefix);
				explorers.put(taskName, new SimpleTaskExplorer(factoryBean));
			} else {
				logger.warn("Cannot find {} in {} ", propertyName, properties.getComposedTaskAppProperties());
			}
		}
		return new TaskExplorerContainer(explorers, taskExplorer);
	}

	@Bean
	public org.springframework.cloud.dataflow.composedtaskrunner.ComposedRunnerJobFactory composedTaskJob(ComposedTaskProperties properties) {

		return new org.springframework.cloud.dataflow.composedtaskrunner.ComposedRunnerJobFactory(properties);
	}

	@Bean
	public TaskExecutor taskExecutor(ComposedTaskProperties properties) {
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(properties.getSplitThreadCorePoolSize());
		taskExecutor.setMaxPoolSize(properties.getSplitThreadMaxPoolSize());
		taskExecutor.setKeepAliveSeconds(properties.getSplitThreadKeepAliveSeconds());
		taskExecutor.setAllowCoreThreadTimeOut(
				properties.isSplitThreadAllowCoreThreadTimeout());
		taskExecutor.setQueueCapacity(properties.getSplitThreadQueueCapacity());
		taskExecutor.setWaitForTasksToCompleteOnShutdown(
				properties.isSplitThreadWaitForTasksToCompleteOnShutdown());
		return taskExecutor;
	}

	@Bean
	public BatchConfigurer getComposedBatchConfigurer(BatchProperties properties,
													  DataSource dataSource, TransactionManagerCustomizers transactionManagerCustomizers,
													  ComposedTaskProperties composedTaskProperties) {
		return new org.springframework.cloud.dataflow.composedtaskrunner.ComposedBatchConfigurer(properties,
				dataSource, transactionManagerCustomizers, composedTaskProperties);
	}
}
