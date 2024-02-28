/*
 * Copyright 2022-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.core.StreamDefinitionService;
import org.springframework.cloud.dataflow.server.EnableDataFlowServer;
import org.springframework.cloud.dataflow.server.service.SchedulerService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskExecutionService;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.scheduler.Scheduler;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;

import static org.mockito.Mockito.mock;

/**
 * A configuration that can be used to bring up an empty Dataflow server with defaults.
 *
 * @author Chris Bono
 */
@Configuration
@EnableAutoConfiguration(exclude = {
		SessionAutoConfiguration.class,
		FlywayAutoConfiguration.class,
		SecurityAutoConfiguration.class,
		SecurityFilterAutoConfiguration.class,
		ManagementWebSecurityAutoConfiguration.class })
@EnableDataFlowServer
public class EmptyDefaultTestApplication {

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
	public TaskExecutionService taskService() {
		return mock(DefaultTaskExecutionService.class);
	}

	@Bean
	public TaskRepository taskRepository() {
		return mock(SimpleTaskRepository.class);
	}

	@Bean
	public SchedulerService schedulerService() {
		return mock(SchedulerService.class);
	}

	@Bean
	public Scheduler scheduler() {
		return mock(Scheduler.class);
	}

	@Bean
	public OAuth2TokenUtilsService oauth2TokenUtilsService() {
		return mock(OAuth2TokenUtilsService.class);
	}

	@Bean
	public StreamDefinitionService streamDefinitionService() {
		return mock(StreamDefinitionService.class);
	}
}
