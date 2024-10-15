/*
 * Copyright 2015-2023 the original author or authors.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Base64Utils;
import org.springframework.cloud.dataflow.core.LaunchResponse;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskManifest;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionMetadataDao;
import org.springframework.cloud.dataflow.server.repository.DuplicateTaskException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionMissingExternalIdException;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExecutionQueryDao;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.cloud.task.listener.TaskExecutionException;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.AdditionalMatchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Daniel Serleg
 * @author David Turanski
 * @author Chris Schaefer
 * @author Corneil du Plessis
 */
@SpringBootTest(classes = {TaskServiceDependencies.class}, properties = {"spring.main.allow-bean-definition-overriding=true"})
@ExtendWith(OutputCaptureExtension.class)
public abstract class DefaultTaskExecutionServiceTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "-ORIG";

	private final static String TASK_NAME_ORIG2 = BASE_TASK_NAME + "-ORIG2";

	private final static String K8_PLATFORM = "k8platform";

	@Autowired
	TaskRepository taskRepository;

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Autowired
	TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	AppRegistryService appRegistry;

	@Autowired
	TaskLauncher taskLauncher;

	@Autowired
	TaskSaveService taskSaveService;

	@Autowired
	TaskDeleteService taskDeleteService;

	@Autowired
	TaskExecutionService taskExecutionService;

	@Autowired
	DataflowTaskExplorer taskExplorer;

	@Autowired
	LauncherRepository launcherRepository;

	@Autowired
	TaskValidationService taskValidationService;

	@Autowired
	AuditRecordService auditRecordService;

	@Autowired
	TaskDeploymentRepository taskDeploymentRepository;

	@Autowired
	TaskExecutionCreationService taskExecutionRepositoryService;

	@Autowired
	TaskAppDeploymentRequestCreator taskAppDeploymentRequestCreator;

	@Autowired
	DataflowTaskExecutionDao dataflowTaskExecutionDao;

	@Autowired
	DataflowTaskExecutionMetadataDao dataflowTaskExecutionMetadataDao;

	@Autowired
	DataflowTaskExecutionQueryDao dataflowTaskExecutionQueryDao;

	@Autowired
	TaskConfigurationProperties taskConfigurationProperties;

	@Autowired
	ApplicationContext applicationContext;

	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	class SimpleDefaultPlatformTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		DataSource dataSource;

		@BeforeEach
		void setup() {
			setupTest(dataSource);
		}

		@Test
		@DirtiesContext
		void executeSingleTaskDefaultsToExistingSinglePlatformTest() {
			initializeSuccessfulRegistry(appRegistry);
			ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			when(taskLauncher.launch(argument.capture())).thenReturn("0");
			validateBasicProperties(Collections.emptyMap(), argument, "default");
		}

		@Test
		@DirtiesContext
		void executeSingleTaskDefaultsToExistingSinglePlatformTestForKubernetes() {
			this.launcherRepository.save(new Launcher(K8_PLATFORM, TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE, taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			when(taskLauncher.launch(argument.capture())).thenReturn("0");
			Map<String, String> taskDeploymentProperties = new HashMap<>();
			taskDeploymentProperties.put("spring.cloud.dataflow.task.platformName", K8_PLATFORM);
			validateBasicProperties(taskDeploymentProperties, argument, K8_PLATFORM);
		}

		@Test
		@DirtiesContext
		void failedFirstLaunch() throws Exception {
			this.launcherRepository.save(new Launcher(TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			TaskExecution taskExecution = new TaskExecution(1, 0, TASK_NAME_ORIG, LocalDateTime.now(), LocalDateTime.now(), "", Collections.emptyList(), "", null, null);
			taskRepository.createTaskExecution(taskExecution);
			TaskManifest taskManifest = new TaskManifest();
			taskManifest.setPlatformName("Cloud Foundry");
			AppDefinition taskDefinition = new AppDefinition(TASK_NAME_ORIG, null);
			AppDeploymentRequest taskDeploymentRequest = new AppDeploymentRequest(taskDefinition, new FileUrlResource("src/test/resources/apps"));
			taskManifest.setTaskDeploymentRequest(taskDeploymentRequest);
			dataflowTaskExecutionMetadataDao.save(taskExecution, taskManifest);
			ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			when(taskLauncher.launch(argument.capture())).thenReturn("0");
			Map<String, String> taskDeploymentProperties = new HashMap<>();
			taskDeploymentProperties.put("spring.cloud.dataflow.task.platformName", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE);
			validateBasicProperties(taskDeploymentProperties, argument, TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE);

		}

		private void validateBasicProperties(Map<String, String> taskDeploymentProperties, ArgumentCaptor<AppDeploymentRequest> argument, String platform) {
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, taskDeploymentProperties, new LinkedList<>());
			AppDeploymentRequest appDeploymentRequest = argument.getValue();
			assertThat(appDeploymentRequest.getDefinition().getProperties()).containsKey("spring.datasource.username");
			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId("0");
			assertThat(taskDeployment).isNotNull();
			assertThat(taskDeployment.getTaskDeploymentId()).isEqualTo("0");
			assertThat(taskDeployment.getTaskDefinitionName()).isEqualTo(TASK_NAME_ORIG);
			assertThat(taskDeployment.getPlatformName()).isEqualTo(platform);
			assertThat(taskDeployment.getCreatedOn()).isNotNull();
		}
	}

	@SuppressWarnings("SqlWithoutWhere")
	public void setupTest(DataSource dataSource) {
		JdbcTemplate template = new JdbcTemplate(dataSource);

		template.execute("DELETE FROM TASK_TASK_BATCH");
		template.execute("DELETE FROM TASK_EXECUTION_PARAMS");
		template.execute("DELETE FROM TASK_EXECUTION;");
		assertThat(this.launcherRepository.findByName("default")).isNull();
		this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
		this.taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		taskDefinitionRepository.findAll();

	}

	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	@TestPropertySource(properties = {"spring.cloud.dataflow.task.use-kubernetes-secrets-for-db-credentials=true"})
	class SimpleDefaultPlatformForKubernetesTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		DataSource dataSource;

		@BeforeEach
		void setup() {
			setupTest(dataSource);
			this.launcherRepository.save(new Launcher(K8_PLATFORM, TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE, taskLauncher));
		}

		@Test
		@DirtiesContext
		void executeSingleTaskDefaultsToExistingSinglePlatformTestForKubernetes() {
			final String K8_PLATFORM = "k8platform";
			initializeSuccessfulRegistry(appRegistry);
			ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			when(taskLauncher.launch(argument.capture())).thenReturn("0");
			Map<String, String> taskDeploymentProperties = new HashMap<>();
			taskDeploymentProperties.put("spring.cloud.dataflow.task.platformName", K8_PLATFORM);
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, taskDeploymentProperties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			AppDeploymentRequest appDeploymentRequest = argument.getValue();
			assertThat(appDeploymentRequest.getDefinition().getProperties().containsKey("spring.datasource.username")).isFalse();
			TaskExecution taskExecution = taskExplorer.getTaskExecution(launchResponse.getExecutionId());
			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId(taskExecution.getExternalExecutionId());
			assertThat(taskDeployment).isNotNull();
			assertThat(taskDeployment.getTaskDeploymentId()).isEqualTo("0");
			assertThat(taskDeployment.getTaskDefinitionName()).isEqualTo(TASK_NAME_ORIG);
			assertThat(taskDeployment.getPlatformName()).isEqualTo(K8_PLATFORM);
			assertThat(taskDeployment.getCreatedOn()).isNotNull();
		}
	}

	@TestPropertySource(properties = {"spring.cloud.dataflow.task.maximum-concurrent-tasks=10"})
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	class CICDTaskTests extends DefaultTaskExecutionServiceTests {

		private Launcher launcher;

		@BeforeEach
		void setup() {
			this.launcher = this.launcherRepository.findByName("default");
			if (this.launcher != null) {
				this.launcherRepository.delete(this.launcher);
			}
			this.launcher = this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));

			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
			taskDefinitionRepository.save(new TaskDefinition("t1", "timestamp"));
			taskDefinitionRepository.save(new TaskDefinition("t2", "l1:timestamp"));
			taskDefinitionRepository.findAll();
		}

		@Test
		@DirtiesContext
		void taskLaunchRequestUnderUpgrade() {
			assertThatThrownBy(() -> {
				Map<String, List<String>> tasksBeingUpgraded = (Map<String, List<String>>) ReflectionTestUtils.getField(this.taskExecutionService, "tasksBeingUpgraded");
				assertThat(tasksBeingUpgraded).isNotNull();
				tasksBeingUpgraded.put("myTask", Collections.singletonList("default"));
				this.taskExecutionService.executeTask("myTask", Collections.emptyMap(), Collections.emptyList());
			}).isInstanceOf(IllegalStateException.class);
		}

		@Test
		@DirtiesContext
		void upgradeDueToResourceChangeForCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));

			setupUpgradeDueToResourceChange();
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void upgradeDueToResourceChangeForOther() throws IOException {
			setupUpgradeDueToResourceChange();
			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeDueToResourceChange() throws IOException {
			initializeSuccessfulRegistry(appRegistry);

			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null), new FileUrlResource("src/test/resources/apps"));
			manifest.setTaskDeploymentRequest(request);
			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, LocalDateTime.now(), null);


			when(taskLauncher.launch(any())).thenReturn("0");

			this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());

			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);
			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
		}

		@Test
		@DirtiesContext
		void restoreAppPropertiesV2() throws IOException {
			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0", "1");

			Map<String, String> properties = new HashMap<>(1);
			properties.put("app.demo.foo", "bar");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, properties, new LinkedList<>());
			long firstTaskExecutionId = launchResponse.getExecutionId();
			taskRepository.completeTaskExecution(firstTaskExecutionId, 0, LocalDateTime.now(), "all done");
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, Collections.emptyMap(), new LinkedList<>());
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);

			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).hasSize(1);
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).containsEntry("app.demo.foo", "bar");

			verify(this.taskLauncher, never()).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void savesRequestedVersionNoLabel() throws IOException {
			initializeMultiVersionRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0", "1");

			Map<String, String> properties = new HashMap<>(1);
			properties.put("version.timestamp", "1.0.1");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask("t1", properties, new LinkedList<>());
			long firstTaskExecutionId = launchResponse.getExecutionId();
			taskRepository.completeTaskExecution(firstTaskExecutionId, 0, LocalDateTime.now(), "all done");
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest("t1");

			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task101");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).hasSize(1);
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).containsEntry("version.timestamp", "1.0.1");

			verify(this.taskLauncher, never()).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void restoresNonDefaultVersion() throws IOException {
			initializeMultiVersionRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0", "1");

			Map<String, String> properties = new HashMap<>(1);
			properties.put("version.timestamp", "1.0.1");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask("t1", properties, new LinkedList<>());
			long firstTaskExecutionId = launchResponse.getExecutionId();
			taskRepository.completeTaskExecution(firstTaskExecutionId, 0, LocalDateTime.now(), "all done");
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest("t1");

			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task101");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).hasSize(1);
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).containsEntry("version.timestamp", "1.0.1");

			properties.clear();
			LaunchResponse launchResponse2 = this.taskExecutionService.executeTask("t1", properties, new LinkedList<>());
			long secondTaskExecutionId = launchResponse2.getExecutionId();

			taskRepository.completeTaskExecution(secondTaskExecutionId, 0, LocalDateTime.now(), "all done");
			lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest("t1");
			// without passing version, we should not get back to default app, in this case foo-task100
			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task101");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).hasSize(1);
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).containsEntry("version.timestamp", "1.0.1");

			verify(this.taskLauncher, never()).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void savesRequestedVersionLabel() throws IOException {
			initializeMultiVersionRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0", "1");

			Map<String, String> properties = new HashMap<>(1);
			properties.put("version.l1", "1.0.1");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask("t2", properties, new LinkedList<>());
			long firstTaskExecutionId = launchResponse.getExecutionId();
			taskRepository.completeTaskExecution(firstTaskExecutionId, 0, LocalDateTime.now(), "all done");
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest("t2");

			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task101");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).hasSize(1);
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).containsEntry("version.l1", "1.0.1");

			verify(this.taskLauncher, never()).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void restoreDeployerPropertiesV2() throws IOException {
			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0", "1");

			Map<String, String> properties = new HashMap<>(1);
			properties.put("deployer.demo.memory", "100000g");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, properties, new LinkedList<>());
			long firstTaskExecutionId = launchResponse.getExecutionId();
			taskRepository.completeTaskExecution(firstTaskExecutionId, 0, LocalDateTime.now(), "all done");
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, Collections.emptyMap(), new LinkedList<>());
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);

			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).hasSize(1);
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).containsEntry("deployer.demo.memory", "100000g");

			verify(this.taskLauncher, never()).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void upgradeDueToDeploymentPropsChangeForCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			setupUpgradeDueToDeploymentPropsChangeForCloudFoundry();
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void upgradeDueToDeploymentPropsChangeForCloudFoundryFailsWhenAlreadyRunning() throws IOException {
			this.launcherRepository.delete(this.launcher);
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null), new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.updateExternalExecutionId(myTask.getExecutionId(), "abc");
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			when(this.taskLauncher.status("abc")).thenReturn(new TaskStatus("abc", LaunchState.running, new HashMap<>()));

			assertThatThrownBy(() -> {
				this.taskExecutionService.executeTask(TASK_NAME_ORIG, Collections.emptyMap(), Collections.emptyList());
			}).isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Unable to update application due to currently running applications");
		}

		@Test
		@DirtiesContext
		void upgradeDueToDeploymentPropsChangeForCloudFoundrySucceedsIfNotReallyRunning() throws IOException {
			this.launcherRepository.delete(this.launcher);
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null), new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.updateExternalExecutionId(myTask.getExecutionId(), "abc");
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			when(this.taskLauncher.status("abc")).thenReturn(new TaskStatus("abc", LaunchState.failed, new HashMap<>()));
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void upgradeDueToDeploymentPropsChangeForOther() throws IOException {
			setupUpgradeDueToDeploymentPropsChangeForCloudFoundry();
			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeDueToDeploymentPropsChangeForCloudFoundry() throws IOException {
			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null), new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, LocalDateTime.now(), null);
			taskRepository.updateExternalExecutionId(myTask.getExecutionId(), "0");

			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> deploymentProperties = new HashMap<>(1);
			deploymentProperties.put("deployer.demo.memory", "10000g");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, new LinkedList<>());
			long taskExecutionId = launchResponse.getExecutionId();
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.findManifestById(taskExecutionId);

			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).hasSize(1);
			assertThat(lastManifest.getTaskDeploymentRequest().getDeploymentProperties()).containsEntry("deployer.demo.memory", "10000g");

		}

		@Test
		@DirtiesContext
		void upgradeDueToAppPropsChangeCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			setupUpgradeForAppPropsChange();
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void commandLineArgChangeCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			this.setupUpgradeForCommandLineArgsChange();

			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void commandLineArgChangeOther() throws IOException {
			this.setupUpgradeForCommandLineArgsChange();

			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeForCommandLineArgsChange() throws IOException {
			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null), new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, LocalDateTime.now(), null);

			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> deploymentProperties = new HashMap<>(1);

			this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, Collections.singletonList("--foo=bar"));
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments()).hasSize(2);
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments().get(0)).isEqualTo("--foo=bar");

			this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, Collections.emptyList());
			lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments()).hasSize(1);
		}

		@Test
		@DirtiesContext
		void commandLineArgAppPrefixes() throws IOException {
			this.setupCommandLineArgAppPrefixes();

			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupCommandLineArgAppPrefixes() throws IOException {
			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null), new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, LocalDateTime.now(), null);

			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> deploymentProperties = new HashMap<>(1);

			this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, Collections.singletonList("app.demo.1=--foo=bar"));
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments()).hasSize(2);
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments().get(0)).isEqualTo("--foo=bar");
		}

		@Test
		@DirtiesContext
		void upgradeDueToAppPropsChangeOther() throws IOException {
			setupUpgradeForAppPropsChange();
			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeForAppPropsChange() throws IOException {
			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null), new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, LocalDateTime.now(), null);

			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> deploymentProperties = new HashMap<>(1);
			deploymentProperties.put("app.demo.foo", "bar");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, new LinkedList<>());
			long taskExecutionId = launchResponse.getExecutionId();
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.findManifestById(taskExecutionId);

			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			assertThat(lastManifest.getTaskDeploymentRequest().getDefinition().getProperties()).hasSize(7);
			assertThat(lastManifest.getTaskDeploymentRequest().getDefinition().getProperties()).containsEntry("foo", "bar");
		}

		@Test
		@DirtiesContext
		void upgradeFailureTaskCurrentlyRunning() throws MalformedURLException {

			// given
			this.launcherRepository.delete(this.launcher);
			this.launcherRepository.save(new Launcher("default", "Cloud Foundry", taskLauncher));
			TaskExecution myTask = taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);

			dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, LocalDateTime.now(), new ArrayList<>(), null);
			taskRepository.updateExternalExecutionId(myTask.getExecutionId(), "abc");
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			when(this.taskLauncher.status("abc")).thenReturn(new TaskStatus("abc", LaunchState.running, new HashMap<>()));
			initializeSuccessfulRegistry(appRegistry);
			// when
			Map<String, String> deploymentProperties = new HashMap<>(1);
			deploymentProperties.put("app.demo.foo", "bar");

			// then
			assertThatThrownBy(() -> {
				this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, Collections.emptyList());
			})
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Unable to update application due to currently running applications");
		}
	}

	@TestPropertySource(properties = {"spring.cloud.dataflow.task.maximum-concurrent-tasks=10"})
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	public class SimpleTaskTests extends DefaultTaskExecutionServiceTests {

		@BeforeEach
		void setup() {
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));

			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG2, "l2:demo2"));
		}

		@Test
		@DirtiesContext
		void createSimpleTask(CapturedOutput outputCapture) {
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			verifyTaskExistsInRepo("simpleTask", "AAA --foo=bar", taskDefinitionRepository);
			taskDeleteService.deleteTaskDefinition("simpleTask", true);
			String logEntries = outputCapture.toString();
			assertThat(logEntries).contains("Deleted task app resources for");
		}

		@Test
		@DirtiesContext
		void executeSingleTaskTest(CapturedOutput outputCapture) {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			TaskExecution taskExecution = this.taskExplorer.getTaskExecution(launchResponse.getExecutionId());
			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId(taskExecution.getExternalExecutionId());
			assertThat(taskDeployment).isNotNull();
			assertThat(taskDeployment.getTaskDefinitionName()).isEqualTo(TASK_NAME_ORIG);
			assertThat(taskDeployment.getPlatformName()).isEqualTo("default");
			assertThat(taskDeployment.getCreatedOn()).isNotNull();
			taskDeleteService.deleteTaskDefinition(TASK_NAME_ORIG, true);
			String logEntries = outputCapture.toString();
			assertThat(logEntries).doesNotContain("Deleted task app resources for");
			assertThat(logEntries).doesNotContain("Attempted delete of app resources for");
		}

		@Test
		@DirtiesContext
		void executeSingleTaskWithPropertiesAppNameTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			Map<String, String> taskDeploymentProperties = new HashMap<>();
			taskDeploymentProperties.put("app.demo.format", "yyyy");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, taskDeploymentProperties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			TaskExecution taskExecution = taskExplorer.getTaskExecution(launchResponse.getExecutionId());
			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId(taskExecution.getExternalExecutionId());
			assertThat(taskDeployment).isNotNull();
			assertThat(taskDeployment.getTaskDefinitionName()).isEqualTo(TASK_NAME_ORIG);
			assertThat(taskDeployment.getPlatformName()).isEqualTo("default");
			assertThat(taskDeployment.getCreatedOn()).isNotNull();
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(taskLauncher, times(1)).launch(argumentCaptor.capture());
			assertThat(argumentCaptor.getValue().getDeploymentProperties()).containsEntry("app.demo.format", "yyyy");
		}

		@Test
		@DirtiesContext
		void executeSingleTaskWithPropertiesAppLabelTest() {
			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);

			Map<String, String> taskDeploymentProperties = new HashMap<>();
			taskDeploymentProperties.put("app.l2.format", "yyyy");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG2, taskDeploymentProperties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			TaskExecution taskExecution = taskExplorer.getTaskExecution(launchResponse.getExecutionId());
			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId(taskExecution.getExternalExecutionId());
			assertThat(taskDeployment).isNotNull();
			assertThat(taskDeployment.getTaskDefinitionName()).isEqualTo(TASK_NAME_ORIG2);
			assertThat(taskDeployment.getPlatformName()).isEqualTo("default");
			assertThat(taskDeployment.getCreatedOn()).isNotNull();

			verify(taskLauncher, times(1)).launch(argumentCaptor.capture());
			assertThat(argumentCaptor.getValue().getDeploymentProperties()).containsEntry("app.l2.format", "yyyy");
		}

		@Test
		@DirtiesContext
		void executeStopTaskTest(CapturedOutput outputCapture) {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(1L);
			taskExecutionService.stopTaskExecution(executionIds);
			String logEntries = outputCapture.toString();
			assertThat(logEntries).contains("Task execution stop request for id 1 for platform default has been submitted");
		}

		@Test
		@DirtiesContext
		void executeStopTaskTestForChildApp(CapturedOutput outputCapture) {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			TaskExecution taskExecution = new TaskExecution(2L, 0, "childTask", LocalDateTime.now(), LocalDateTime.now(), "", Collections.emptyList(), "", "1234A", 1L);
			taskRepository.createTaskExecution(taskExecution);
			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(2L);
			taskExecutionService.stopTaskExecution(executionIds);
			String logEntries = outputCapture.toString();
			assertThat(logEntries).contains("Task execution stop request for id 2 for platform default has been submitted");
		}

		@Test
		@DirtiesContext
		void executeStopTaskTestAppNoPlatform() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			TaskExecution taskExecution = new TaskExecution(2L, 0, "childTask", LocalDateTime.now(), LocalDateTime.now(), "", Collections.emptyList(), "", "1234A", null);
			taskRepository.createTaskExecution(taskExecution);
			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(2L);
			assertThatThrownBy(() -> {
				taskExecutionService.stopTaskExecution(executionIds);
			}).isInstanceOf(TaskExecutionException.class).hasMessageContaining("No platform could be found for task execution id 2");
		}

		@Test
		@DirtiesContext
		void executeStopForSpecificPlatformTaskTest(CapturedOutput outputCapture) {
			this.launcherRepository.save(new Launcher("MyPlatform", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			this.launcherRepository.delete(this.launcherRepository.findByName("default"));
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(1L);
			taskExecutionService.stopTaskExecution(executionIds, "MyPlatform");
			String logEntries = outputCapture.toString();
			assertThat(logEntries).contains("Task execution stop request for id 1 for platform MyPlatform has been submitted");
		}

		@Test
		@DirtiesContext
		void executeStopTaskWithNoChildExternalIdTest() {
			initializeSuccessfulRegistry(this.appRegistry);
			when(this.taskLauncher.launch(any())).thenReturn("0");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);

			TaskExecution taskExecution = taskRepository.createTaskExecution();
			taskRepository.startTaskExecution(taskExecution.getExecutionId(), "invalidChildTaskExecution", LocalDateTime.now(), Collections.emptyList(), null, 1L);
			validateFailedTaskStop(2);
		}

		@Test
		@DirtiesContext
		void executeStopTaskWithNoExternalIdTest() {
			taskRepository.createTaskExecution("invalidExternalTaskId");
			validateFailedTaskStop(1);
		}

		private void validateFailedTaskStop(long id) {
			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(1L);
			assertThatThrownBy(() -> {
				this.taskExecutionService.stopTaskExecution(executionIds);

			}).isInstanceOf(TaskExecutionMissingExternalIdException.class).hasMessageContaining(String.format("The TaskExecutions with the following ids: %s do not have external execution ids.", id));
		}

		@Test()
		@DirtiesContext
		void executeStopInvalidIdTaskTest() {
			assertThatThrownBy(() -> {
				initializeSuccessfulRegistry(appRegistry);
				when(taskLauncher.launch(any())).thenReturn("0");
				LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
				assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
				Set<Long> executionIds = new HashSet<>(2);
				executionIds.add(1L);
				executionIds.add(5L);
				taskExecutionService.stopTaskExecution(executionIds);
			}).isInstanceOf(NoSuchTaskExecutionException.class);
		}

		@Test
		@DirtiesContext
		void executeMultipleTasksTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(2L);
		}

		@Test
		@DirtiesContext
		void getTaskLog() {
			String platformName = "test-platform";
			String taskDefinitionName = "test";
			String taskDeploymentId = "12345";
			TaskDeployment taskDeployment = new TaskDeployment();
			taskDeployment.setPlatformName(platformName);
			taskDeployment.setTaskDefinitionName(taskDefinitionName);
			taskDeployment.setTaskDeploymentId(taskDeploymentId);
			this.launcherRepository.save(new Launcher(platformName, TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			when(taskLauncher.getLog(taskDeploymentId)).thenReturn("Logs");
			assertThat(this.taskExecutionService.getLog(taskDeployment.getPlatformName(), taskDeploymentId)).isEqualTo("Logs");
		}

		@Test
		@DirtiesContext
		void getCFTaskLog() {
			String platformName = "cf-test-platform";
			String taskDefinitionName = "test";
			String taskDeploymentId = "12345";
			TaskDeployment taskDeployment = new TaskDeployment();
			taskDeployment.setPlatformName(platformName);
			taskDeployment.setTaskDefinitionName(taskDefinitionName);
			taskDeployment.setTaskDeploymentId(taskDeploymentId);
			this.taskDeploymentRepository.save(taskDeployment);
			this.launcherRepository.save(new Launcher(platformName, TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			when(taskLauncher.getLog("12345")).thenReturn("Logs");
			assertThat(this.taskExecutionService.getLog(taskDeployment.getPlatformName(), taskDeploymentId)).isEqualTo("Logs");
		}

		@Test
		@DirtiesContext
		void getCFTaskLogByInvalidTaskId() {
			String platformName = "cf-test-platform";
			String taskDeploymentId = "12345";
			TaskLauncher taskLauncherCF = mock(TaskLauncher.class);
			when(taskLauncherCF.getLog(any())).thenThrow(new IllegalArgumentException("could not find a GUID app id for the task guid id"));
			this.launcherRepository.save(new Launcher(platformName, TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncherCF));
			assertThat(this.taskExecutionService.getLog(platformName, taskDeploymentId)).isEqualTo("Log could not be retrieved.  Verify that deployments are still available.");
		}

		@Test
		@DirtiesContext
		void getCFTaskLogByTaskIdOtherThanLatest() {
			String taskName = "test-task";
			String platformName = "cf-test-platform";
			String taskDeploymentId = "12345";
			TaskDeployment taskDeployment = new TaskDeployment();
			taskDeployment.setPlatformName(platformName);
			taskDeployment.setTaskDeploymentId(taskDeploymentId);
			taskDeployment.setTaskDefinitionName(taskName);
			this.taskDeploymentRepository.save(taskDeployment);
			TaskExecution taskExecution = new TaskExecution();
			taskExecution.setStartTime(LocalDateTime.now());
			taskExecution.setTaskName(taskName);
			taskExecution.setExternalExecutionId("12346");
			taskRepository.createTaskExecution(taskExecution);
			this.launcherRepository.save(new Launcher(platformName, TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			assertThat(this.taskExecutionService.getLog(platformName, taskDeploymentId)).isEmpty();
		}

		@Test
		@DirtiesContext
		void executeSameTaskDefinitionWithInvalidPlatform() {
			this.launcherRepository.delete(launcherRepository.findByName("default"));
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> deploymentProperties = new HashMap<>();
			deploymentProperties.put(DefaultTaskExecutionService.TASK_PLATFORM_NAME, "noplatformhere");

			assertThatThrownBy(() -> {
				this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, new LinkedList<>());
			}).isInstanceOf(IllegalStateException.class).hasMessageContaining("No launcher was available for platform noplatformhere");
		}

		@Test
		@DirtiesContext
		void executeSameTaskDefinitionOnMultiplePlatforms() {
			initializeSuccessfulRegistry(appRegistry);
			if (this.launcherRepository.findByName("default") == null) {
				this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			}
			if (this.launcherRepository.findByName("anotherPlatform") == null) {
				this.launcherRepository.save(new Launcher("anotherPlatform", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			}
			when(taskLauncher.launch(any())).thenReturn("0");

			LaunchResponse launchResponse = this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			Map<String, String> deploymentProperties = new HashMap<>();
			deploymentProperties.put(DefaultTaskExecutionService.TASK_PLATFORM_NAME, "anotherPlatform");
			assertThatThrownBy(() -> this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, new LinkedList<>())).isInstanceOf(IllegalStateException.class).hasMessageContaining("Task definition [" + TASK_NAME_ORIG + "] has already been deployed on platform [default].  Requested to deploy on platform [anotherPlatform].");
		}

		@Test
		@DirtiesContext
		void executeDeleteNoDeploymentWithMultiplePlatforms(CapturedOutput outputCapture) {
			this.launcherRepository.save(new Launcher("MyPlatform", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			this.launcherRepository.save(new Launcher("anotherPlatform", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			taskDeleteService.deleteTaskDefinition(TASK_NAME_ORIG, true);
			String logEntries = outputCapture.toString();
			assertThat(logEntries).contains("Deleted task app resources for " + TASK_NAME_ORIG + " in platform anotherPlatform");
			assertThat(logEntries).contains("Deleted task app resources for " + TASK_NAME_ORIG + " in platform default");
			assertThat(logEntries).contains("Deleted task app resources for " + TASK_NAME_ORIG + " in platform MyPlatform");
		}

		@Test
		@DirtiesContext
		void executeTaskWithNullIDReturnedTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(this.taskLauncher.launch(any())).thenReturn(null);
			assertThatThrownBy(() -> {
				taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			})
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Deployment ID is null for the task:" + TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void executeTaskWithNullDefinitionTest() {
			when(this.taskLauncher.launch(any())).thenReturn("0");
			TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties = new ComposedTaskRunnerConfigurationProperties();
			TaskExecutionInfoService taskExecutionInfoService = new DefaultTaskExecutionInfoService(this.dataSourceProperties, this.appRegistry, this.taskExplorer, mock(TaskDefinitionRepository.class), taskConfigurationProperties, mock(LauncherRepository.class), Collections.singletonList(mock(TaskPlatform.class)), composedTaskRunnerConfigurationProperties);
			TaskExecutionService taskExecutionService = new DefaultTaskExecutionService(applicationContext.getEnvironment(),
				launcherRepository, auditRecordService, taskRepository, taskExecutionInfoService,
				mock(TaskDeploymentRepository.class), taskDefinitionRepository,
				taskExecutionRepositoryService, taskAppDeploymentRequestCreator, this.taskExplorer,
				this.dataflowTaskExecutionDao, this.dataflowTaskExecutionMetadataDao,
				this.dataflowTaskExecutionQueryDao, mock(OAuth2TokenUtilsService.class), this.taskSaveService,
				taskConfigurationProperties, composedTaskRunnerConfigurationProperties);
			assertThatThrownBy(() -> taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>())).isInstanceOf(NoSuchTaskDefinitionException.class).hasMessageContaining("Could not find task definition named " + TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		void validateValidTaskTest() {
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertThat(validationStatus.getAppsStatuses()).containsEntry("task:simpleTask", "valid");
		}

		@DirtiesContext
		public void validateMissingTaskDefinitionTest() {
			assertThatThrownBy(() -> {
				initializeSuccessfulRegistry(appRegistry);
				ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
				assertThat(validationStatus.getAppsStatuses()).containsEntry("task:simpleTask", "valid");
			}).isInstanceOf(NoSuchTaskDefinitionException.class);
		}

		@Test
		@DirtiesContext
		void validateInvalidTaskTest() {
			initializeFailRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertThat(validationStatus.getAppsStatuses()).containsEntry("task:simpleTask", "invalid");
		}

		@Test
		@DirtiesContext
		void validateInvalidTaskNameTest() {
			String[] taskNames = {"$task", "task$", "ta_sk"};

			for (String taskName : taskNames) {
				assertThatThrownBy(() -> {
					initializeSuccessfulRegistry(appRegistry);
					taskSaveService.saveTaskDefinition(new TaskDefinition(taskName, "AAA --foo=bar"));
					this.launcherRepository.save(new Launcher("k8s1", TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE, taskLauncher));
					this.launcherRepository.save(new Launcher("cf1", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
					initializeSuccessfulRegistry(appRegistry);
					Map<String, String> taskDeploymentProperties = new HashMap<>();
					taskDeploymentProperties.put("spring.cloud.dataflow.task.platformName", "k8s1");
					taskExecutionService.executeTask(taskName, taskDeploymentProperties, Collections.emptyList());
				}).isInstanceOf(TaskException.class).hasMessageContaining("Task name " + taskName + " is invalid. Task name must consist of " + "alphanumeric characters or '-', start with an alphabetic character, and end with an " + "alphanumeric character (e.g. 'my-name', or 'abc-123')");
			}
			taskDeleteService.deleteAll();
			for (String taskName : taskNames) {
				try {
					initializeSuccessfulRegistry(appRegistry);
					taskSaveService.saveTaskDefinition(new TaskDefinition(taskName, "AAA --foo=bar"));
					this.launcherRepository.save(new Launcher("k8s1", TaskPlatformFactory.KUBERNETES_PLATFORM_TYPE, taskLauncher));
					this.launcherRepository.save(new Launcher("cf1", TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
					initializeSuccessfulRegistry(appRegistry);
					Map<String, String> taskDeploymentProperties = new HashMap<>();
					taskDeploymentProperties.put("spring.cloud.dataflow.task.platformName", "cf1");
					taskExecutionService.executeTask(taskName, taskDeploymentProperties, Collections.emptyList());
				} catch (TaskException e) {
					fail("TaskException is not expected");
				} catch (IllegalStateException e) {
					// Ignore for the tests
				}
				taskDeleteService.deleteAll();
			}
		}

		@Test
		@DirtiesContext
		void validateNullResourceTaskTest() {
			initializeNullRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertThat(validationStatus.getAppsStatuses()).containsEntry("task:simpleTask", "invalid");
		}
	}

	@TestPropertySource(properties = {"spring.cloud.dataflow.task.auto-create-task-definitions=true"})
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	class AutoCreateTaskDefinitionTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		TaskDefinitionRepository taskDefinitionRepository;

		@Autowired
		TaskExecutionInfoService taskExecutionInfoService;

		@BeforeEach
		void setup() {
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
		}

		@Test
		@DirtiesContext
		void executeTaskWithNullDefinitionCreatesDefinitionIfConfigured() {
			initializeSuccessfulRegistry(appRegistry);
			when(this.taskLauncher.launch(any())).thenReturn("0");
			taskExecutionService.executeTask("demo", new HashMap<>(), new LinkedList<>());
			assertThat(taskDefinitionRepository.findByTaskName("demo")).isNotNull();
		}
	}

	@TestPropertySource(properties = {"spring.cloud.dataflow.applicationProperties.task.globalkey=globalvalue", "spring.cloud.dataflow.applicationProperties.stream.globalstreamkey=nothere"})
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	public class TaskTests extends DefaultTaskExecutionServiceTests {

		public static final String TIMESTAMP_3 = "timestamp3";

		@Autowired
		TaskDefinitionRepository taskDefinitionRepository;

		@BeforeEach
		void setup() throws MalformedURLException {
			when(appRegistry.find(eq(TIMESTAMP_3), eq(ApplicationType.task))).thenReturn(new AppRegistration(TIMESTAMP_3, ApplicationType.task, "3.0.0", URI.create("https://timestamp3"), null));
			when(appRegistry.find(not(eq(TIMESTAMP_3)), any(ApplicationType.class))).thenReturn(new AppRegistration("some-task", ApplicationType.task, "1.0.0", URI.create("https://timestamp3"), null));
			when(appRegistry.getAppResource(any())).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task"));
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
		}

		@Test
		@DirtiesContext
		void launchCheckProperties() throws IOException {
			this.taskDefinitionRepository.save(new TaskDefinition(TIMESTAMP_3, TIMESTAMP_3));
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			this.taskExecutionService.executeTask(TIMESTAMP_3, new HashMap<>(), new LinkedList<>());
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest(TIMESTAMP_3);
			assertThat(lastManifest).as("expected to find manifest for " + TIMESTAMP_3).isNotNull();
			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			System.out.println("cmdLine:" + lastManifest.getTaskDeploymentRequest().getCommandlineArguments());
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments()).hasSize(1);
		}

		@Test
		@DirtiesContext
		void launchWithName() throws IOException {
			this.taskDefinitionRepository.save(new TaskDefinition("ts3", TIMESTAMP_3));
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			this.taskExecutionService.executeTask("ts3", new HashMap<>(), new LinkedList<>());
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest("ts3");
			assertThat(lastManifest).as("expected to find manifest for ts3").isNotNull();
			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			System.out.println("cmdLine:" + lastManifest.getTaskDeploymentRequest().getCommandlineArguments());
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments()).hasSize(1);
		}

		@Test
		@DirtiesContext
		void launchWithNameAndVersion() throws IOException {
			DefaultTaskExecutionServiceTests.initializeMultiVersionRegistry(appRegistry);
			this.taskDefinitionRepository.save(new TaskDefinition("ts3", "s1: some-name"));
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			LaunchResponse response = this.taskExecutionService.executeTask("ts3", Collections.singletonMap("version.s1", "1.0.2"), new LinkedList<>());
			this.taskExecutionService.findTaskManifestById(response.getExecutionId());
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest("ts3");
			assertThat(lastManifest).as("expected to find manifest for ts3").isNotNull();
			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task102");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			System.out.println("cmdLine:" + lastManifest.getTaskDeploymentRequest().getCommandlineArguments());
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments()).hasSize(1);
		}

		@Test
		@DirtiesContext
		void launchWithVersion() throws IOException {
			DefaultTaskExecutionServiceTests.initializeMultiVersionRegistry(appRegistry);
			this.taskDefinitionRepository.save(new TaskDefinition("s3", "some-name"));
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			LaunchResponse response = this.taskExecutionService.executeTask("s3", Collections.emptyMap(), Collections.emptyList());
			this.taskExecutionService.findTaskManifestById(response.getExecutionId());
			when(this.taskLauncher.launch(any())).thenReturn("xyz");
			response = this.taskExecutionService.executeTask("s3", Collections.singletonMap("version.some-name", "1.0.2"), new LinkedList<>());
			this.taskExecutionService.findTaskManifestById(response.getExecutionId());
			TaskManifest lastManifest = dataflowTaskExecutionMetadataDao.getLatestManifest("s3");
			assertThat(lastManifest).as("expected to find manifest for s3").isNotNull();
			assertThat(lastManifest.getTaskDeploymentRequest().getResource().getURL()).hasToString("file:src/test/resources/apps/foo-task102");
			assertThat(lastManifest.getPlatformName()).isEqualTo("default");
			System.out.println("cmdLine:" + lastManifest.getTaskDeploymentRequest().getCommandlineArguments());
			assertThat(lastManifest.getTaskDeploymentRequest().getCommandlineArguments()).hasSize(1);
		}
	}

	@TestPropertySource(properties = {
			"spring.cloud.dataflow.applicationProperties.task.globalkey=globalvalue",
			"spring.cloud.dataflow.applicationProperties.stream.globalstreamkey=nothere"
	})
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	class ComposedTaskTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		TaskRepository taskRepository;

		@Autowired
		DataSourceProperties dataSourceProperties;

		@Autowired
		private TaskDefinitionRepository taskDefinitionRepository;

		@Autowired
		private AppRegistryService appRegistry;

		@Autowired
		private TaskLauncher taskLauncher;

		@Autowired
		private LauncherRepository launcherRepository;

		@Autowired
		private TaskExecutionService taskExecutionService;

		@BeforeEach
		void setupMocks() {
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			this.launcherRepository.save(new Launcher("MyPlatform", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
		}

		@Test
		@DirtiesContext
		void executeComposedTask() {
			AppDeploymentRequest request = prepComposedTaskRunner(null);
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "seqTask");
			assertThat(request.getDefinition().getProperties()).containsKey("composed-task-properties");
			assertThat(request.getDefinition().getProperties()).containsEntry("composed-task-properties", "app.seqTask-AAA.app.AAA.timestamp.format=YYYY, deployer.seqTask-AAA.deployer.AAA.memory=1240m");
			assertThat(request.getDefinition().getProperties()).containsKey("interval-time-between-checks");
			assertThat(request.getDefinition().getProperties()).containsEntry("interval-time-between-checks", "1000");
			assertThat(request.getDefinition().getProperties()).doesNotContainKey("app.foo");
			assertThat(request.getDefinition().getProperties()).containsEntry("globalkey", "globalvalue");
			assertThat(request.getDefinition().getProperties().get("globalstreamkey")).isNull();
			assertThat(request.getDefinition().getProperties()).containsEntry("platform-name", "default");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithVersions() throws MalformedURLException {
			AppDeploymentRequest request = prepComposedTaskRunnerWithVersions(null);
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "seqTask");
			assertThat(request.getDefinition().getProperties()).containsKey("composed-task-properties");
			assertThat(request.getDefinition().getProperties()).containsEntry("composed-task-properties", "version.seqTask-t1.t1=1.0.0, version.seqTask-t2.t2=1.0.1");
			assertThat(request.getDefinition().getProperties()).containsEntry("globalkey", "globalvalue");
			assertThat(request.getDefinition().getProperties().get("globalstreamkey")).isNull();
			assertThat(request.getDefinition().getProperties()).containsEntry("platform-name", "default");
		}

		private AppDeploymentRequest prepComposedTaskRunnerWithVersions(String platformName) throws MalformedURLException {
			String dsl = "t1:some-name && t2:some-name";
			initializeMultiVersionRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");
			when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);
			Map<String, String> properties = new HashMap<>();
			if (StringUtils.hasText(platformName)) {
				properties.put("spring.cloud.dataflow.task.platformName", platformName);
			}
			properties.put("version.t1", "1.0.0");
			properties.put("version.t2", "1.0.1");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());
			return argumentCaptor.getValue();
		}

		@Test
		@DirtiesContext
		void executeComposedTaskNewPlatform() {
			AppDeploymentRequest request = prepComposedTaskRunner("MyPlatform");
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "seqTask");
			assertThat(request.getDefinition().getProperties()).containsKey("composed-task-properties");
			assertThat(request.getDefinition().getProperties()).containsEntry("composed-task-properties", "app.seqTask-AAA.app.AAA.timestamp.format=YYYY, deployer.seqTask-AAA.deployer.AAA.memory=1240m");
			assertThat(request.getDefinition().getProperties()).containsKey("interval-time-between-checks");
			assertThat(request.getDefinition().getProperties()).containsEntry("interval-time-between-checks", "1000");
			assertThat(request.getDefinition().getProperties()).doesNotContainKey("app.foo");
			assertThat(request.getDefinition().getProperties()).containsEntry("globalkey", "globalvalue");
			assertThat(request.getDefinition().getProperties().get("globalstreamkey")).isNull();
			assertThat(request.getDefinition().getProperties()).containsEntry("platform-name", "MyPlatform");
		}

		private AppDeploymentRequest prepComposedTaskRunner(String platformName) {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");
			when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);
			Map<String, String> properties = new HashMap<>();
			if (StringUtils.hasText(platformName)) {
				properties.put("spring.cloud.dataflow.task.platformName", platformName);
			}
			properties.put("app.foo", "bar");
			properties.put("app.seqTask.AAA.timestamp.format", "YYYY");
			properties.put("deployer.seqTask.AAA.memory", "1240m");
			properties.put("app.composed-task-runner.interval-time-between-checks", "1000");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());
			return argumentCaptor.getValue();
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithAccessTokenDisabled1() {
			initializeSuccessfulRegistry(appRegistry);
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService, this.taskLauncher, this.appRegistry), Collections.emptyList(), this.taskExecutionService, this.taskLauncher);
			assertThat(request.getDefinition().getProperties()).doesNotContainKey("dataflow-server-access-token");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithAccessTokenDisabled2() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			arguments.add("--dataflow-server-use-user-access-token=false");
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService, this.taskLauncher, this.appRegistry), Collections.emptyList(), this.taskExecutionService, this.taskLauncher);
			assertThat(request.getDefinition().getProperties()).doesNotContainKey("dataflow-server-access-token");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithEnabledUserAccessToken1() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			arguments.add("--dataflow-server-use-user-access-token=true");
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService, this.taskLauncher, this.appRegistry), arguments, this.taskExecutionService, this.taskLauncher);
			assertThat(request.getDefinition().getProperties()).containsKey("dataflow-server-access-token");
			assertThat(request.getDefinition().getProperties()).containsEntry("dataflow-server-access-token", "foo-bar-123-token");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithEnabledUserAccessToken2() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			arguments.add("--dataflow-server-use-user-access-token =  true");
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService, this.taskLauncher, this.appRegistry), arguments, this.taskExecutionService, this.taskLauncher);
			assertThat(request.getDefinition().getProperties()).containsKey("dataflow-server-access-token");
			assertThat(request.getDefinition().getProperties()).containsEntry("dataflow-server-access-token", "foo-bar-123-token");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithAccessTokenOverrideAsProperty() {
			initializeSuccessfulRegistry(appRegistry);

			Map<String, String> properties = prepareEnvironmentForTokenTests(this.taskSaveService, this.taskLauncher, this.appRegistry);
			properties.put("app.composed-task-runner.dataflow-server-access-token", "foo-bar-123-token-override");

			AppDeploymentRequest request = getAppDeploymentRequestForToken(properties, Collections.emptyList(), this.taskExecutionService, this.taskLauncher);

			assertThat(request.getDefinition().getProperties()).containsKey("dataflow-server-access-token");

			boolean containsArgument = false;
			for (String argument : request.getCommandlineArguments()) {
				if (argument.contains("--dataflow-server-access-token")) {
					containsArgument = true;
					break;
				}
			}

			assertThat(containsArgument).isFalse();
			assertThat(request.getDefinition().getProperties()).containsEntry("dataflow-server-access-token", "foo-bar-123-token-override");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithAccessTokenOverrideAsArgument() {
			initializeSuccessfulRegistry(appRegistry);

			List<String> args = Collections.singletonList("--dataflow-server-access-token=foo-bar-123-token-override");

			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService, this.taskLauncher, this.appRegistry), args, this.taskExecutionService, this.taskLauncher);

			assertThat(request.getDefinition().getProperties()).doesNotContainKey("dataflow-server-access-token");

			boolean containsArgument = false;
			String argumentValue = null;
			for (String argument : request.getCommandlineArguments()) {
				if (argument.contains("--dataflow-server-access-token")) {
					containsArgument = true;
					argumentValue = argument;
				}
			}
			assertThat(request.getCommandlineArguments()).doesNotContain("dataflow-server-access-token");
			assertThat(containsArgument).isTrue();
			assertThat(argumentValue).isEqualTo("--dataflow-server-access-token=foo-bar-123-token-override");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskwithUserCTRName() {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);
			when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> properties = new HashMap<>();
			properties.put("app.foo", "bar");
			properties.put("app.seqTask.AAA.timestamp.format", "YYYY");
			properties.put("deployer.seqTask.AAA.memory", "1240m");
			properties.put("app.composed-task-runner.interval-time-between-checks", "1000");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

			AppDeploymentRequest request = argumentCaptor.getValue();
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "seqTask");
			assertThat(request.getDefinition().getProperties()).containsKey("composed-task-properties");
			assertThat(request.getCommandlineArguments()).contains("--spring.cloud.data.flow.taskappname=composed-task-runner");
			assertThat(request.getDefinition().getProperties()).containsEntry("composed-task-properties", "app.seqTask-AAA.app.AAA.timestamp.format=YYYY, deployer.seqTask-AAA.deployer.AAA.memory=1240m");
			assertThat(request.getDefinition().getProperties()).containsKey("interval-time-between-checks");
			assertThat(request.getDefinition().getProperties()).containsEntry("interval-time-between-checks", "1000");
			assertThat(request.getDefinition().getProperties()).doesNotContainKey("app.foo");
			assertThat(request.getDefinition().getProperties()).containsEntry("globalkey", "globalvalue");
			assertThat(request.getDefinition().getProperties().get("globalstreamkey")).isNull();
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithUserCTRNameTask() {
			String dsl = "a1: AAA && b2: BBB";
			when(appRegistry.find(eq("AAA"), eq(ApplicationType.task))).thenReturn(new AppRegistration("AAA", ApplicationType.task, "3.0.0", URI.create("https://helloworld"), null));
			when(appRegistry.find(not(eq("AAA")), any(ApplicationType.class))).thenReturn(new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
			try {
				when(appRegistry.getAppResource(any())).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task"));
			} catch (MalformedURLException e) {
				throw new IllegalStateException("Invalid File Resource Specified", e);
			}
			when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
			when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> properties = new HashMap<>();
			properties.put("app.foo", "bar");
			properties.put("app.seqTask.AAA.timestamp.format", "YYYY");
			properties.put("deployer.seqTask.AAA.memory", "1240m");
			properties.put("app.composed-task-runner.interval-time-between-checks", "1000");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

			AppDeploymentRequest request = argumentCaptor.getValue();
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "seqTask");

			assertThat(request.getCommandlineArguments()).contains("--spring.cloud.data.flow.taskappname=composed-task-runner");
			assertThat(request.getDeploymentProperties()).containsEntry("app.seqTask.AAA.timestamp.format", "YYYY");
			assertThat(request.getDeploymentProperties()).containsEntry("deployer.seqTask.AAA.memory", "1240m");
			System.out.println("definitionProperties:" + request.getDefinition().getProperties());
			assertThat(request.getDefinition().getProperties()).containsKey("interval-time-between-checks");
			assertThat(request.getDefinition().getProperties()).containsEntry("interval-time-between-checks", "1000");
			assertThat(request.getDefinition().getProperties()).doesNotContainKey("app.foo");
			assertThat(request.getDefinition().getProperties()).containsEntry("globalkey", "globalvalue");
			assertThat(request.getDefinition().getProperties().get("globalstreamkey")).isNull();
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithEnd() {
			String dsl = "timestamp '*'->t1: timestamp 'FOO'->$END";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("transitionTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> properties = new HashMap<>();
			properties.put("app.t1.timestamp.format", "YYYY");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("transitionTask", properties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

			AppDeploymentRequest request = argumentCaptor.getValue();
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "transitionTask");
			String keyWithEncoding = "composed-task-app-properties." + Base64Utils.encode("app.t1.timestamp.format");
			assertThat(request.getDefinition().getProperties()).containsEntry(keyWithEncoding, "YYYY");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithLabels() {
			String dsl = "t1: AAA && t2: BBB";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> properties = new HashMap<>();
			properties.put("app.seqTask.t1.timestamp.format", "YYYY");
			properties.put("app.composed-task-runner.interval-time-between-checks", "1000");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

			AppDeploymentRequest request = argumentCaptor.getValue();
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "seqTask");
			assertThat(request.getDefinition().getProperties()).containsKey("composed-task-properties");
			assertThat(request.getDefinition().getProperties()).containsEntry("composed-task-properties", "app.seqTask-t1.app.AAA.timestamp.format=YYYY");
			assertThat(request.getDefinition().getProperties()).containsKey("interval-time-between-checks");
			assertThat(request.getDefinition().getProperties()).containsEntry("interval-time-between-checks", "1000");
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithLabelsV2() {
			String dsl = "t1: AAA && t2: BBB";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> properties = new HashMap<>();
			properties.put("app.t1.timestamp.format", "YYYY");
			List<String> arguments = new ArrayList<>();
			arguments.add("app.t1.0=foo1");
			arguments.add("app.*.0=foo2");
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("seqTask", properties, arguments);
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

			AppDeploymentRequest request = argumentCaptor.getValue();
			System.out.println("request.definition.properties:" + request.getDefinition().getProperties());
			System.out.println("request.commandLineArguments:" + request.getCommandlineArguments());
			assertThat(request.getDefinition().getProperties()).containsEntry("spring.cloud.task.name", "seqTask");
			assertThat(request.getDefinition().getProperties()).containsEntry("composed-task-app-properties." + Base64Utils.encode("app.t1.timestamp.format"), "YYYY");
			assertThat(request.getCommandlineArguments()).contains("--composed-task-app-arguments." + Base64Utils.encode("app.t1.0") + "=foo1");
			assertThat(request.getCommandlineArguments()).contains("--composed-task-app-arguments." + Base64Utils.encode("app.*.0") + "=foo2");

		}

		@Test
		@DirtiesContext
		void createSequenceComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA && BBB";
			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			verifyTaskExistsInRepo("seqTask", dsl, taskDefinitionRepository);

			verifyTaskExistsInRepo("seqTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("seqTask-BBB", "BBB", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		void createSplitComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "<AAA || BBB>";
			taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			verifyTaskExistsInRepo("splitTask", dsl, taskDefinitionRepository);

			verifyTaskExistsInRepo("splitTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("splitTask-BBB", "BBB", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		void verifyComposedTaskFlag() {
			String composedTaskDsl = "<AAA || BBB>";
			assertThat(TaskServiceUtils.isComposedTaskDefinition(composedTaskDsl)).isTrue();
			composedTaskDsl = "AAA 'FAILED' -> BBB '*' -> CCC";
			assertThat(TaskServiceUtils.isComposedTaskDefinition(composedTaskDsl)).isTrue();
			composedTaskDsl = "AAA && BBB && CCC";
			assertThat(TaskServiceUtils.isComposedTaskDefinition(composedTaskDsl)).isTrue();
			String nonComposedTaskDsl = "AAA";
			assertThat(TaskServiceUtils.isComposedTaskDefinition(nonComposedTaskDsl)).isFalse();
			nonComposedTaskDsl = "AAA --foo=bar";
			assertThat(TaskServiceUtils.isComposedTaskDefinition(nonComposedTaskDsl)).isFalse();
		}

		@Test
		@DirtiesContext
		void verifyComposedTaskConcurrentCountExceeded() {
			String dsl = "<AAA || BBB>";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.getMaximumConcurrentTasks()).thenReturn(20);
			when(taskLauncher.launch(any())).thenReturn("0");
			when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);
			Map<String, String> properties = new HashMap<>();
			LaunchResponse launchResponse = this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>());
			assertThat(launchResponse.getExecutionId()).isEqualTo(1L);

			initializeSuccessfulRegistry(appRegistry);
			dsl = "<AAA||BBB||CCC>&&<AAA1||BBB1||CCC1||DDD1 --foo=bar||DDD2||DDD3||DDD4||DDD5||DDD6||" + "DDD7||DDD8||DDD9||DDD10||DDD11||DDD12||DDD13||DDD14||DDD15||DDD16||" + "DDD17||DDD18>&&<AAA2||BBB2>";
			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask1", dsl));
			try {
				this.taskExecutionService.executeTask("seqTask1", properties, new LinkedList<>());
			} catch (IllegalArgumentException iae) {
				assertThat(iae.getMessage()).isEqualTo("One or more of the splits in the composed task contains " + "a task count that exceeds the maximumConcurrentTasks count of 20");
				return;
			}
			fail("Expected IllegalArgumentException maxConcurrentTasks exceeded was not thrown");
		}

		@Test
		@DirtiesContext
		void createTransitionComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA 'FAILED' -> BBB '*' -> CCC";
			taskSaveService.saveTaskDefinition(new TaskDefinition("transitionTask", dsl));
			verifyTaskExistsInRepo("transitionTask", dsl, taskDefinitionRepository);

			verifyTaskExistsInRepo("transitionTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("transitionTask-BBB", "BBB", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		void deleteAllComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String taskDsl1 = "AAA && BBB && CCC";
			String taskDsl2 = "DDD";
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask1", taskDsl1));
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask2", taskDsl2));
			verifyTaskExistsInRepo("deleteTask1-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask1-BBB", "BBB", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask1-CCC", "CCC", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask1", taskDsl1, taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask2", taskDsl2, taskDefinitionRepository);

			long preDeleteSize = taskDefinitionRepository.count();
			taskDeleteService.deleteAll();
			assertThat(preDeleteSize - 5).isEqualTo(taskDefinitionRepository.count());
		}

		@Test
		@DirtiesContext
		void deleteComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA && BBB && CCC";
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask", dsl));
			verifyTaskExistsInRepo("deleteTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-BBB", "BBB", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-CCC", "CCC", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask", dsl, taskDefinitionRepository);

			long preDeleteSize = taskDefinitionRepository.count();
			taskDeleteService.deleteTaskDefinition("deleteTask");
			assertThat(preDeleteSize - 4).isEqualTo(taskDefinitionRepository.count());
		}

		@Test
		@DirtiesContext
		void deleteComposedTaskMissingChildTasks() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA && BBB && CCC";
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask", dsl));
			verifyTaskExistsInRepo("deleteTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-BBB", "BBB", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-CCC", "CCC", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask", dsl, taskDefinitionRepository);
			taskDeleteService.deleteTaskDefinition("deleteTask-BBB");
			long preDeleteSize = taskDefinitionRepository.count();
			taskDeleteService.deleteTaskDefinition("deleteTask");
			assertThat(preDeleteSize - 3).isEqualTo(taskDefinitionRepository.count());
		}

		@Test
		@DirtiesContext
		void deleteComposedTaskDeleteOnlyChildren() {
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask-AAA", "AAA"));
			String dsl = "BBB && CCC";
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask", dsl));
			verifyTaskExistsInRepo("deleteTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-BBB", "BBB", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-CCC", "CCC", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask", dsl, taskDefinitionRepository);

			long preDeleteSize = taskDefinitionRepository.count();
			taskDeleteService.deleteTaskDefinition("deleteTask");
			assertThat(preDeleteSize - 3).isEqualTo(taskDefinitionRepository.count());
			verifyTaskExistsInRepo("deleteTask-AAA", "AAA", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		void deleteComposedTaskWithLabel() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "LLL: AAA && BBB";
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask", dsl));
			verifyTaskExistsInRepo("deleteTask-LLL", "LLL:AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-BBB", "BBB", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask", dsl, taskDefinitionRepository);

			long preDeleteSize = taskDefinitionRepository.count();
			taskDeleteService.deleteTaskDefinition("deleteTask");
			assertThat(preDeleteSize - 3).isEqualTo(taskDefinitionRepository.count());
		}

		@Test
		@DirtiesContext
		void createFailedComposedTask() {
			String dsl = "AAA && BBB";
			initializeFailRegistry(appRegistry);
			assertThatThrownBy(() -> {
				taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			}).isInstanceOf(IllegalArgumentException.class);
			assertThat(wasTaskDefinitionCreated("splitTask", taskDefinitionRepository)).isFalse();
			assertThat(wasTaskDefinitionCreated("splitTask-AAA", taskDefinitionRepository)).isFalse();
			assertThat(wasTaskDefinitionCreated("splitTask-BBB", taskDefinitionRepository)).isFalse();
		}

		@Test
		@DirtiesContext
		void createDuplicateComposedTask() {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			assertThatThrownBy(() -> {
				taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			}).isInstanceOf(DuplicateTaskException.class);
			assertThat(wasTaskDefinitionCreated("splitTask", taskDefinitionRepository)).isTrue();
			assertThat(wasTaskDefinitionCreated("splitTask-AAA", taskDefinitionRepository)).isTrue();
			assertThat(wasTaskDefinitionCreated("splitTask-BBB", taskDefinitionRepository)).isTrue();
		}

		@Test
		@DirtiesContext
		void createDuplicateChildTaskComposedTask() {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask-BBB", "BBB"));
			assertThatThrownBy(() -> {
				taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			}).isInstanceOf(DuplicateTaskException.class);
			assertThat(wasTaskDefinitionCreated("splitTask", taskDefinitionRepository)).isFalse();
			assertThat(wasTaskDefinitionCreated("splitTask-AAA", taskDefinitionRepository)).isFalse();
			assertThat(wasTaskDefinitionCreated("splitTask-BBB", taskDefinitionRepository)).isTrue();
		}
	}

	@TestPropertySource(properties = {"spring.cloud.dataflow.applicationProperties.task.globalkey=globalvalue", "spring.cloud.dataflow.applicationProperties.stream.globalstreamkey=nothere", "spring.cloud.dataflow.task.useUserAccessToken=true"})
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@Nested
	class ComposedTaskWithSystemUseUserAccessTokenTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		TaskRepository taskRepository;

		@Autowired
		DataSourceProperties dataSourceProperties;

		@Autowired
		private AppRegistryService appRegistry;

		@Autowired
		private TaskLauncher taskLauncher;

		@Autowired
		private LauncherRepository launcherRepository;

		@Autowired
		private TaskExecutionService taskExecutionService;

		@BeforeEach
		void setupMocks() {
			assertThat(this.launcherRepository.findByName("default")).isNull();
			this.launcherRepository.save(new Launcher("default", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
			this.launcherRepository.save(new Launcher("MyPlatform", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, taskLauncher));
		}

		@Test
		@DirtiesContext
		void executeComposedTaskWithEnabledUserAccessToken1() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService, this.taskLauncher, this.appRegistry), arguments, this.taskExecutionService, this.taskLauncher);
			assertThat(request.getDefinition().getProperties()).containsKey("dataflow-server-access-token");
			assertThat(request.getDefinition().getProperties()).containsEntry("dataflow-server-access-token", "foo-bar-123-token");
		}
	}

	static AppDeploymentRequest getAppDeploymentRequestForToken(Map<String, String> taskDeploymentProperties, List<String> commandLineArgs, TaskExecutionService taskExecutionService, TaskLauncher taskLauncher) {
		LaunchResponse launchResponse = taskExecutionService.executeTask("seqTask", taskDeploymentProperties, commandLineArgs);
		assertThat(launchResponse.getExecutionId()).isEqualTo(1L);

		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

		final AppDeploymentRequest request = argumentCaptor.getValue();

		return request;
	}

	static Map<String, String> prepareEnvironmentForTokenTests(TaskSaveService taskSaveService, TaskLauncher taskLauncher, AppRegistryService appRegistry) {
		taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", "AAA && BBB"));
		when(taskLauncher.launch(any())).thenReturn("0");
		when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);
		Map<String, String> properties = new HashMap<>();
		properties.put("app.foo", "bar");
		properties.put("app.seqTask.AAA.timestamp.format", "YYYY");
		properties.put("deployer.seqTask.AAA.memory", "1240m");
		properties.put("app.composed-task-runner.interval-time-between-checks", "1000");
		return properties;
	}

	private static void initializeSuccessfulRegistry(AppRegistryService appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		try {
			when(appRegistry.getAppResource(any())).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task"));
		} catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid File Resource Specified", e);
		}
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}

	private static void initializeMultiVersionRegistry(AppRegistryService appRegistry) throws MalformedURLException {
		AppRegistration appRegistration100 = new AppRegistration("some-name", ApplicationType.task, "1.0.0", URI.create("https://helloworld/some-name-1.0.0.jar"), null);
		AppRegistration appRegistration101 = new AppRegistration("some-name", ApplicationType.task, "1.0.1", URI.create("https://helloworld/some-name-1.0.1.jar"), null);
		AppRegistration appRegistration102 = new AppRegistration("some-name", ApplicationType.task, "1.0.2", URI.create("https://helloworld/some-name-1.0.2.jar"), null);
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(appRegistration100);
		when(appRegistry.find(anyString(), any(ApplicationType.class), eq("1.0.0"))).thenReturn(appRegistration100);
		when(appRegistry.find(anyString(), any(ApplicationType.class), eq("1.0.1"))).thenReturn(appRegistration101);
		when(appRegistry.find(anyString(), any(ApplicationType.class), eq("1.0.2"))).thenReturn(appRegistration102);

		ArgumentMatcher<AppRegistration> versionMatcher100 = ap -> ap != null && ap.getVersion() != null && ap.getVersion().equals("1.0.0");
		ArgumentMatcher<AppRegistration> versionMatcher101 = ap -> ap != null && ap.getVersion() != null && ap.getVersion().equals("1.0.1");
		ArgumentMatcher<AppRegistration> versionMatcher102 = ap -> ap != null && ap.getVersion() != null && ap.getVersion().equals("1.0.2");
		ArgumentMatcher<AppRegistration> ctrMatcher = ap -> ap != null && ap.getName().equals("composed-task-runner");
		when(appRegistry.getAppResource(argThat(versionMatcher100))).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task100"));
		when(appRegistry.getAppResource(argThat(versionMatcher101))).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task101"));
		when(appRegistry.getAppResource(argThat(versionMatcher102))).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task102"));
		when(appRegistry.getAppResource(argThat(ctrMatcher))).thenReturn(new FileUrlResource("src/test/resources/apps/ctr"));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}

	private static void initializeFailRegistry(AppRegistryService appRegistry) throws IllegalArgumentException {
		when(appRegistry.find("BBB", ApplicationType.task)).thenThrow(new IllegalArgumentException(String.format("Application name '%s' with type '%s' does not exist in the app registry.", "fake", ApplicationType.task)));
		when(appRegistry.find("AAA", ApplicationType.task)).thenReturn(new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task/bad.jar"));
	}

	private static void initializeNullRegistry(AppRegistryService appRegistry) throws IllegalArgumentException {
		when(appRegistry.find("BBB", ApplicationType.task)).thenThrow(new IllegalArgumentException(String.format("Application name '%s' with type '%s' does not exist in the app registry.", "fake", ApplicationType.task)));
		when(appRegistry.find("AAA", ApplicationType.task)).thenReturn(mock(AppRegistration.class));
	}

	private static void verifyTaskExistsInRepo(String taskName, String dsl, TaskDefinitionRepository taskDefinitionRepository) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findById(taskName).get();
		assertThat(taskDefinition.getName()).isEqualTo(taskName);
		assertThat(taskDefinition.getDslText()).isEqualTo(dsl);
	}

	private static boolean wasTaskDefinitionCreated(String taskName, TaskDefinitionRepository taskDefinitionRepository) {
		return taskDefinitionRepository.findById(taskName).isPresent();
	}
}
