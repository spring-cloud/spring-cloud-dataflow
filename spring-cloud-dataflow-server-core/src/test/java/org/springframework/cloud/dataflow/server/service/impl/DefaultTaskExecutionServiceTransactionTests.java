/*
 * Copyright 2019-2020 the original author or authors.
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

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
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
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@AutoConfigureTestDatabase(replace = Replace.ANY)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class DefaultTaskExecutionServiceTransactionTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
	TaskExplorer taskExplorer;

	@Autowired
	TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties;

	@Autowired
	DataflowTaskExecutionDao dataflowTaskExecutionDao;

	@Autowired
	DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao;

	private TaskExecutionService transactionTaskService;

	@Before
	public void setupMocks() {
		this.launcherRepository.save(new Launcher("default", "local", new TaskLauncherStub(dataSource)));
		this.taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		this.taskDefinitionRepository.findAll();
		this.transactionTaskService = new DefaultTaskExecutionService(
				launcherRepository, auditRecordService, taskRepository,
				taskExecutionInfoService, mock(TaskDeploymentRepository.class),
				taskExecutionRepositoryService, taskAppDeploymentRequestCreator,
				this.taskExplorer, this.dataflowTaskExecutionDao, this.dataflowTaskExecutionMetadataDao,
				mock(OAuth2TokenUtilsService.class), this.taskSaveService, this.taskConfigurationProperties,
				this.composedTaskRunnerConfigurationProperties);
	}

	@Test
	@DirtiesContext
	public void executeSingleTaskTransactionTest() {
		initializeSuccessfulRegistry(this.appRegistry);
		assertEquals(1L, this.transactionTaskService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));
	}

	private static class TaskLauncherStub implements TaskLauncher {
		private String result = "0";

		private DataSource dataSource;

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

	private static class AuditRecordServiceStub implements AuditRecordService {

		@Override
		public AuditRecord populateAndSaveAuditRecord(AuditOperationType auditOperationType, AuditActionType auditActionType, String correlationId, String data, String platformName) {
			return null;
		}

		@Override
		public AuditRecord populateAndSaveAuditRecordUsingMapData(AuditOperationType auditOperationType, AuditActionType auditActionType, String correlationId, Map<String, Object> data, String platformName) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public Page<AuditRecord> findAuditRecordByAuditOperationTypeAndAuditActionTypeAndDate(Pageable pageable, AuditActionType[] actions, AuditOperationType[] operations, Instant fromDate, Instant toDate) {
			return null;
		}

		@Override
		public Optional<AuditRecord> findById(Long id) {
			return Optional.empty();
		}
	}

	private static void initializeSuccessfulRegistry(AppRegistryService appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task"));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}
}
