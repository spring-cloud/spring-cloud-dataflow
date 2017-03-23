/*
 * Copyright 2016-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.config;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.security.authentication.AuthenticationManager;

/**
 * A test class for testing custom task configuration.
 * 
 * @author Jason Laber
 */
public class DataFlowServerCustomTaskConfigurationTests {

	private AnnotationConfigApplicationContext context;
	private ConfigurableEnvironment environment;

	@Before
	public void setup() {
		context = new AnnotationConfigApplicationContext();
		context.setId("testDataFlowConfig");
		context.register(
				DataFlowServerCustomTaskConfigurationTests.TestConfiguration.class,
				RedisAutoConfiguration.class,
				SecurityAutoConfiguration.class,
				DataFlowServerAutoConfiguration.class,
				DataFlowControllerAutoConfiguration.class,
				DataSourceAutoConfiguration.class,
				DataFlowServerConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		environment = new StandardEnvironment();
	}

	@After
	public void teardown() {
		if(context != null && context.isActive()) {
			context.close();
		}
	}

	/**
	 * Verify that custom task configuration is in place over the default configuration.
	 */
	@Test
	public void testCustomTaskConfiguration(){
		
		context.setEnvironment(environment);
		context.refresh();
		
		assertTrue(context.getBean(TaskService.class).toString().startsWith("Mock for TaskService"));
		assertTrue(context.getBean(TaskJobService.class).toString().startsWith("Mock for TaskJobService"));
		assertTrue(context.getBean(JobService.class).toString().startsWith("Mock for JobService"));
		assertTrue(context.getBean(JobExplorer.class).toString().startsWith("Mock for JobExplorer"));
		assertTrue(context.getBean(JobRepository.class).toString().startsWith("Mock for JobRepository"));
		assertTrue(context.getBean(TaskExplorer.class).toString().startsWith("Mock for TaskExplorer"));
		assertTrue(context.getBean(AppDeployer.class).toString().startsWith("Mock for AppDeployer"));
		assertTrue(context.getBean(TaskLauncher.class).toString().startsWith("Mock for TaskLauncher"));
		assertTrue(context.getBean(AuthenticationManager.class).toString().startsWith("Mock for AuthenticationManager"));
		assertTrue(context.getBean(TaskRepository.class).toString().startsWith("Mock for TaskRepository"));
		assertTrue(context.getBean(TaskDefinitionRepository.class).toString().startsWith("Mock for TaskDefinitionRepository"));
		
	}

	@EnableDataFlowServer
	private static class TestConfiguration {
		
		@Bean
		public TaskService taskService() {
			return mock(TaskService.class);
		}
		
		@Bean
		public TaskJobService taskJobService() {
			return mock(TaskJobService.class);
		}
		
		@Bean
		public JobService jobService() {
			return mock(JobService.class);
		}
		
		@Bean
		public JobExplorer jobExplorer() {
			return mock(JobExplorer.class);
		}
		
		@Bean
		public JobRepository jobRepository() {
			return mock(JobRepository.class);
		}
		
		@Bean
		public TaskExplorer taskExplorer() {
			return mock(TaskExplorer.class);
		}
		
		@Bean
		public AppDeployer appDeployer() {
			return mock(AppDeployer.class);
		}

		@Bean
		public TaskLauncher taskLauncher() {
			return mock(TaskLauncher.class);
		}

		@Bean
		public AuthenticationManager authenticationManager() {
			return mock(AuthenticationManager.class);
		}

		@Bean
		public TaskRepository taskRepository() {
			return mock(TaskRepository.class);
		}
		
		@Bean
		public TaskDefinitionRepository taskDefinitionRepository() {
			return mock(TaskDefinitionRepository.class);
		}
	}
}
