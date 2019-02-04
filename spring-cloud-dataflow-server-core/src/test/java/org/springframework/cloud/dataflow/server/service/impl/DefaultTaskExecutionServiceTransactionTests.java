/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;


/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmbeddedDataSourceConfiguration.class, TaskServiceDependencies.class})
@EnableConfigurationProperties({CommonApplicationProperties.class, TaskConfigurationProperties.class, DockerValidatorProperties.class})
public class DefaultTaskExecutionServiceTransactionTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private DataSourceProperties dataSourceProperties;

	@Autowired
	private TaskExplorer taskExplorer;

	@Autowired
	private TaskRepository taskExecutionRepository;

	@Autowired
	private TaskExecutionCreationService taskExecutionCreationService;

	@Autowired
	private ApplicationConfigurationMetadataResolver metadataResolver;

	@Autowired
	private TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	private CommonApplicationProperties commonApplicationProperties;

	@Autowired
	private TaskValidationService taskValidationService;

	@Autowired
	private DataSource dataSource;

	private TaskService transactionTaskService;

	private TaskLauncherStub taskLauncher;

	@Before
	public void setupMockMVC() {
		taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		this.taskLauncher = new TaskLauncherStub(this.dataSource);

	}

	@Test
	@DirtiesContext
	public void executeSingleTaskTest() {
		this.transactionTaskService = new DefaultTaskService(this.dataSourceProperties, taskDefinitionRepository, taskExplorer,
				taskExecutionRepository, appRegistry, taskLauncher, metadataResolver, taskConfigurationProperties,
				new InMemoryDeploymentIdRepository(), new AuditRecordServiceStub(), null, commonApplicationProperties,
				taskValidationService, this.taskExecutionCreationService);
		initializeSuccessfulRegistry(appRegistry);
		this.transactionTaskService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
		assertThat(taskLauncher.getResult(), is(equalTo("1")));
	}

	private static void initializeSuccessfulRegistry(AppRegistry appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("http://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task"));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
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

		public String getResult() {
			return result;
		}
	}

	private static class AuditRecordServiceStub implements AuditRecordService {

		@Override
		public AuditRecord populateAndSaveAuditRecord(AuditOperationType auditOperationType, AuditActionType auditActionType, String correlationId, String data) {
			return null;
		}

		@Override
		public AuditRecord populateAndSaveAuditRecordUsingMapData(AuditOperationType auditOperationType, AuditActionType auditActionType, String correlationId, Map<String, Object> data) {
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public Page<AuditRecord> findAuditRecordByAuditOperationTypeAndAuditActionType(Pageable pageable, AuditActionType[] actions, AuditOperationType[] operations) {
			return null;
		}

		@Override
		public AuditRecord findOne(Long id) {
			return null;
		}
	}

}
