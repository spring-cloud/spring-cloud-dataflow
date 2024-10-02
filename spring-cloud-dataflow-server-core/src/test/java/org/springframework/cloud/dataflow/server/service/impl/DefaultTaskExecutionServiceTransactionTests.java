/*
 * Copyright 2019-2023 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.LaunchResponse;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = {TaskServiceDependencies.class}, properties = {
		"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureTestDatabase(replace = Replace.ANY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class DefaultTaskExecutionServiceTransactionTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	@Autowired
	TaskRepository taskRepository;

	@Autowired
	TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	AppRegistryService appRegistry;

	@Autowired
	TaskSaveService taskSaveService;

	@Autowired
	TaskExecutionInfoService taskExecutionInfoService;

	@Autowired
	LauncherRepository launcherRepository;

	@Autowired
	AuditRecordService auditRecordService;

	@Autowired
	DataSource dataSource;

	@Autowired
	TaskExecutionCreationService taskExecutionRepositoryService;

	@Autowired
	TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator;

	@Autowired
	DataflowTaskExplorer taskExplorer;

	@Autowired
	TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	@Autowired
	DataflowTaskExecutionDao dataflowTaskExecutionDao;

	@Autowired
	DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao;

	@Autowired
	DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao;

	private TaskExecutionService transactionTaskService;

	@Autowired
	ApplicationContext applicationContext;

	@BeforeEach
	void setupMocks() {
		assertThat(this.launcherRepository.findByName("default")).isNull();
		this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, new TaskLauncherStub(dataSource)));
		this.taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		this.taskDefinitionRepository.findAll();
		this.transactionTaskService = new DefaultTaskExecutionService(
				applicationContext.getEnvironment(),
				launcherRepository,
				auditRecordService,
				taskRepository,
				taskExecutionInfoService,
				mock(TaskDeploymentRepository.class),
				taskDefinitionRepository,
				taskExecutionRepositoryService,
				taskAppDeploymentRequestCreator,
				taskExplorer,
				dataflowTaskExecutionDao,
				dataflowTaskExecutionMetadataDao,
				dataflowTaskExecutionQueryDao,
				mock(OAuth2TokenUtilsService.class),
				taskSaveService,
				taskConfigurationProperties,
				null
		);
	}

	@Test
	@DirtiesContext
	void executeSingleTaskTransactionTest() {
		initializeSuccessfulRegistry(this.appRegistry);
		LaunchResponse taskExecution = this.transactionTaskService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
		assertThat(taskExecution.getExecutionId()).isEqualTo(1L);
	}

	private static class TaskLauncherStub implements TaskLauncher {
		private String result = "0";

		private final DataSource dataSource;

		private TaskLauncherStub(DataSource dataSource) {
			this.dataSource = dataSource;
		}

		@Override
		public String launch(AppDeploymentRequest request) {
			JdbcTemplate template = new JdbcTemplate(this.dataSource);
			result = String.valueOf(template.queryForObject("select count(*) from task_execution", Integer.class));
			return result;
		}

		@Override
		public void cancel(String id) {

		}

		@Override
		public TaskStatus status(String id) {
			return null;
		}

		@Override
		public void cleanup(String id) {

		}

		@Override
		public void destroy(String appName) {

		}

		@Override
		public RuntimeEnvironmentInfo environmentInfo() {
			return null;
		}

		@Override
		public int getMaximumConcurrentTasks() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int getRunningTaskExecutionCount() {
			throw new UnsupportedOperationException();
		}

		@Override
		public String getLog(String taskDeploymentId) {
			return null;
		}

		public String getResult() {
			return result;
		}
	}

	private static void initializeSuccessfulRegistry(AppRegistryService appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task"));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}
}
