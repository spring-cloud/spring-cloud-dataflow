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
import org.springframework.cloud.dataflow.registry.AppRegistryCommon;
import org.springframework.cloud.dataflow.registry.domain.AppRegistration;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.dataflow.server.audit.domain.AuditActionType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditOperationType;
import org.springframework.cloud.dataflow.server.audit.domain.AuditRecord;
import org.springframework.cloud.dataflow.server.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.RdbmsTaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.support.DataflowRdbmsInitializer;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.impl.validation.DefaultTaskValidationService;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.PlatformTransactionManager;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmbeddedDataSourceConfiguration.class, DefaultTaskServiceTransactionTests.TaskServiceDelayDependencies.class})
@EnableConfigurationProperties({CommonApplicationProperties.class, TaskConfigurationProperties.class, DockerValidatorProperties.class})
public class DefaultTaskServiceTransactionTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	private AppRegistry appRegistry;

	@Autowired
	private TaskLauncherStub taskLauncher;

	@Autowired
	private TaskService taskService;

	@Before
	public void setupMockMVC() {
		taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
	}

	@Test
	@DirtiesContext
	public void executeSingleTaskTest() {
		initializeSuccessfulRegistry(appRegistry);
		this.taskService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
		assertThat(taskLauncher.getResult(), is(equalTo("1")));
	}

	private static void initializeSuccessfulRegistry(AppRegistry appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("http://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task"));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}

	public static class TaskServiceDelayDependencies {

		@Autowired
		DataSourceProperties dataSourceProperties;

		@Bean
		public TaskRepositoryInitializer taskExecutionRepository(DataSource dataSource) {
			TaskRepositoryInitializer taskRepositoryInitializer = new TaskRepositoryInitializer();
			taskRepositoryInitializer.setDataSource(dataSource);
			return taskRepositoryInitializer;
		}

		@Bean
		public TaskDefinitionRepository taskDefinitionRepository(DataSource dataSource) {
			return new RdbmsTaskDefinitionRepository(dataSource);
		}

		@Bean
		public TaskValidationService taskValidationService(AppRegistryCommon appRegistryCommon,
				DockerValidatorProperties dockerValidatorProperties,
				TaskDefinitionRepository taskDefinitionRepository,
				TaskConfigurationProperties taskConfigurationProperties) {
			return new DefaultTaskValidationService(appRegistryCommon,
					dockerValidatorProperties,
					taskDefinitionRepository,
					taskConfigurationProperties.getComposedTaskRunnerName());
		}


		@Bean
		public TaskRepository taskRepository(TaskExecutionDaoFactoryBean daoFactoryBean) {
			return new SimpleTaskRepository(daoFactoryBean);
		}

		@Bean
		public AuditRecordService auditRecordService() {
			return new AuditRecordServiceStub();
		}

		@Bean
		public TaskExplorer taskExplorer(TaskExecutionDaoFactoryBean daoFactoryBean) {
			return new SimpleTaskExplorer(daoFactoryBean);
		}

		@Bean
		public TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean(DataSource dataSource) {
			return new TaskExecutionDaoFactoryBean(dataSource);
		}

		@Bean
		public AppRegistry appRegistry() {
			return mock(AppRegistry.class);
		}

		@Bean
		public ResourceLoader resourceLoader() {
			ResourceLoader resourceLoader = mock(ResourceLoader.class);
			when(resourceLoader.getResource(anyString())).thenReturn(mock(Resource.class));
			return resourceLoader;
		}

		@Bean
		TaskLauncher taskLauncher(DataSource dataSource) {
			return new TaskLauncherStub(dataSource);

		}

		@Bean
		ApplicationConfigurationMetadataResolver metadataResolver() {
			return mock(ApplicationConfigurationMetadataResolver.class);
		}

		@Bean
		public DataflowRdbmsInitializer definitionRepositoryInitializer(DataSource dataSource) {
			DataflowRdbmsInitializer definitionRepositoryInitializer = new DataflowRdbmsInitializer(featuresProperties());
			definitionRepositoryInitializer.setDataSource(dataSource);
			return definitionRepositoryInitializer;
		}

		@Bean
		public FeaturesProperties featuresProperties() {
			return new FeaturesProperties();
		}

		@Bean
		public DataSourceTransactionManager transactionManager(DataSource dataSource) {
			return new DataSourceTransactionManager(dataSource);
		}

		@Bean
		public DefaultTaskService defaultTaskService(TaskDefinitionRepository taskDefinitionRepository,
				TaskExplorer taskExplorer, TaskRepository taskExecutionRepository, AppRegistry appRegistry,
				TaskLauncher taskLauncher, ApplicationConfigurationMetadataResolver metadataResolver,
				TaskConfigurationProperties taskConfigurationProperties, AuditRecordService auditRecordService,
				CommonApplicationProperties commonApplicationProperties, TaskValidationService taskValidationService,
				PlatformTransactionManager transactionManager) {
			return new DefaultTaskService(this.dataSourceProperties, taskDefinitionRepository, taskExplorer,
					taskExecutionRepository, appRegistry, taskLauncher, metadataResolver, taskConfigurationProperties,
					new InMemoryDeploymentIdRepository(), auditRecordService, null, commonApplicationProperties,
					taskValidationService, transactionManager);
		}

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
