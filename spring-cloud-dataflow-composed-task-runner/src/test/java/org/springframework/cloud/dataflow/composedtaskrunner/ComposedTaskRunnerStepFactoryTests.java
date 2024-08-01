/*
 * Copyright 2017-2024 the original author or authors.
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


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.common.security.CommonSecurityAutoConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.configuration.DataFlowTestConfiguration;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.task.configuration.TaskConfigurer;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskNameResolver;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskNameResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={EmbeddedDataSourceConfiguration.class,
	DataFlowTestConfiguration.class,StepBeanDefinitionRegistrar.class,
	ComposedTaskRunnerConfiguration.class,
	StepBeanDefinitionRegistrar.class})
@EnableAutoConfiguration(exclude = { CommonSecurityAutoConfiguration.class})
@TestPropertySource(properties = {"graph=FOOBAR","max-wait-time=1000", "increment-instance-enabled=true", "spring.cloud.task.name=footest"})
public class ComposedTaskRunnerStepFactoryTests {

	@Autowired
	ComposedTaskRunnerStepFactory stepFactory;

	@Test
	public void testStep() throws Exception {
		Step step = stepFactory.getObject();
		assertThat(step).isNotNull();
		assertThat(step.getName()).isEqualTo("FOOBAR_0");
		assertThat(step.getStartLimit()).isEqualTo(Integer.MAX_VALUE);
	}

	@Configuration
	public static class StepFactoryConfiguration {

		@MockBean
		public StepExecutionListener composedTaskStepExecutionListener;

		@MockBean
		public TaskOperations taskOperations;

		@Bean
		public ComposedTaskProperties composedTaskProperties() {
			return new ComposedTaskProperties();
		}

		@Bean
		public TaskProperties taskProperties() {
			return new TaskProperties();
		}

		@Bean
		public StepBuilder steps() {
			return new StepBuilder("foo", mock(JobRepository.class));
		}

		@Bean
		public TaskConfigurer taskConfigurer() {
			return new TaskConfigurer() {
				@Override
				public TaskRepository getTaskRepository() {
					return null;
				}

				@Override
				public PlatformTransactionManager getTransactionManager() {
					return null;
				}

				@Override
				public TaskExplorer getTaskExplorer() {
					return mock(TaskExplorer.class);
				}

				@Override
				public DataSource getTaskDataSource() {
					return mock(DataSource.class);
				}

				@Override
				public TaskNameResolver getTaskNameResolver() {
					return new SimpleTaskNameResolver();
				}
			};
		}
	}
}
