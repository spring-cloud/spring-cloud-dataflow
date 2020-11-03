/*
 * Copyright 2015-2020 the original author or authors.
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.OutputCaptureRule;
import org.springframework.cloud.common.security.core.support.OAuth2TokenUtilsService;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
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
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.cloud.task.listener.TaskException;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.FileUrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
public abstract class DefaultTaskExecutionServiceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCaptureRule outputCapture = new OutputCaptureRule();

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	private final static String K8_PLATFORM = "k8platform";

	private final static String FAKE_PLATFORM = "fakeplatformname";

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
	TaskExplorer taskExplorer;

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
	TaskConfigurationProperties taskConfigurationProperties;

	@AutoConfigureTestDatabase(replace = Replace.ANY)
	public static class SimpleDefaultPlatformTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		DataSource dataSource;

		@Before
		public void setup() {
			setupTest(dataSource);
		}

		@Test
		@DirtiesContext
		public void executeSingleTaskDefaultsToExistingSinglePlatformTest() {
			initializeSuccessfulRegistry(appRegistry);
			ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			when(taskLauncher.launch(argument.capture())).thenReturn("0");
			validateBasicProperties(Collections.emptyMap(), argument, FAKE_PLATFORM);
		}

		@Test
		@DirtiesContext
		public void executeSingleTaskDefaultsToExistingSinglePlatformTestForKubernetes() {
			this.launcherRepository.save(new Launcher(K8_PLATFORM, "Kubernetes", taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			when(taskLauncher.launch(argument.capture())).thenReturn("0");
			Map<String, String> taskDeploymentProperties = new HashMap<>();
			taskDeploymentProperties.put("spring.cloud.dataflow.task.platformName", K8_PLATFORM);
			validateBasicProperties(taskDeploymentProperties, argument, K8_PLATFORM);
		}

		private void validateBasicProperties(Map<String, String> taskDeploymentProperties,
				ArgumentCaptor<AppDeploymentRequest> argument,
				String platform) {
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG,
					taskDeploymentProperties, new LinkedList<>()));
			AppDeploymentRequest appDeploymentRequest = argument.getValue();
			assertTrue(appDeploymentRequest.getDefinition().getProperties().containsKey("spring.datasource.username"));
			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId("0");
			assertNotNull("TaskDeployment should not be null", taskDeployment);
			assertEquals("0", taskDeployment.getTaskDeploymentId());
			assertEquals(TASK_NAME_ORIG, taskDeployment.getTaskDefinitionName());
			assertEquals(platform, taskDeployment.getPlatformName());
			assertNotNull("TaskDeployment createdOn field should not be null", taskDeployment.getCreatedOn());
		}
	}

	public void setupTest(DataSource dataSource) {
		this.launcherRepository.save(new Launcher(FAKE_PLATFORM, "local", taskLauncher));
		this.taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		taskDefinitionRepository.findAll();
		JdbcTemplate template = new JdbcTemplate(dataSource);
		template.execute("DELETE FROM TASK_EXECUTION_PARAMS");
		template.execute("DELETE FROM TASK_EXECUTION;");
	}

	@AutoConfigureTestDatabase(replace = Replace.ANY)
	@TestPropertySource(properties = { "spring.cloud.dataflow.task.use-kubernetes-secrets-for-db-credentials=true" })
	public static class SimpleDefaultPlatformForKubernetesTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		DataSource dataSource;

		@Before
		public void setup() {
			setupTest(dataSource);
			this.launcherRepository.save(new Launcher(K8_PLATFORM, "Kubernetes", taskLauncher));
		}

		@Test
		@DirtiesContext
		public void executeSingleTaskDefaultsToExistingSinglePlatformTestForKubernetes() {
			final String K8_PLATFORM = "k8platform";
			initializeSuccessfulRegistry(appRegistry);
			ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			when(taskLauncher.launch(argument.capture())).thenReturn("0");
			Map<String, String> taskDeploymentProperties = new HashMap<>();
			taskDeploymentProperties.put("spring.cloud.dataflow.task.platformName", K8_PLATFORM);
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG,
					taskDeploymentProperties, new LinkedList<>()));
			AppDeploymentRequest appDeploymentRequest = argument.getValue();
			assertFalse(appDeploymentRequest.getDefinition().getProperties().containsKey("spring.datasource.username"));
			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId("0");
			assertNotNull("TaskDeployment should not be null", taskDeployment);
			assertEquals("0", taskDeployment.getTaskDeploymentId());
			assertEquals(TASK_NAME_ORIG, taskDeployment.getTaskDefinitionName());
			assertEquals(K8_PLATFORM, taskDeployment.getPlatformName());
			assertNotNull("TaskDeployment createdOn field should not be null", taskDeployment.getCreatedOn());
		}
	}

	@TestPropertySource(properties = { "spring.cloud.dataflow.task.maximum-concurrent-tasks=10" })
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	public static class CICDTaskTests extends DefaultTaskExecutionServiceTests {

		private Launcher launcher;

		@Before
		public void setup() {
			this.launcher = this.launcherRepository.save(new Launcher("default", "local", taskLauncher));

			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
			taskDefinitionRepository.findAll();
		}

		@Test(expected = IllegalStateException.class)
		@DirtiesContext
		public void testTaskLaunchRequestUnderUpgrade() {
			Map<String, List<String>> tasksBeingUpgraded =
					(Map<String, List<String>>) ReflectionTestUtils.getField(this.taskExecutionService, "tasksBeingUpgraded");

			tasksBeingUpgraded.put("myTask", Arrays.asList("default"));

			this.taskExecutionService.executeTask("myTask", Collections.emptyMap(), Collections.emptyList());
		}

		@Test
		@DirtiesContext
		public void testUpgradeDueToResourceChangeForCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			this.launcherRepository.save(new Launcher("default", "Cloud Foundry", taskLauncher));

			setupUpgradeDueToResourceChange();
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		public void testUpgradeDueToResourceChangeForOther() throws IOException {
			setupUpgradeDueToResourceChange();
			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeDueToResourceChange() throws IOException{
			TaskExecution myTask = this.taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					new FileUrlResource("src/test/resources/apps"));
			manifest.setTaskDeploymentRequest(request);

			this.dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			this.taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, new Date(), new ArrayList<>(), null);
			this.taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, new Date(), null);

			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");

			this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());

			TaskManifest lastManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);
			assertEquals("file:src/test/resources/apps/foo-task", lastManifest.getTaskDeploymentRequest().getResource().getURL().toString());
			assertEquals("default", lastManifest.getPlatformName());
		}

		@Test
		@DirtiesContext
		public void testRestoreAppPropertiesV2() throws IOException {
			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0", "1");

			Map<String, String> properties = new HashMap<>(1);
			properties.put("app.demo.foo", "bar");

			long firstTaskExecutionId = this.taskExecutionService.executeTask(TASK_NAME_ORIG, properties, new LinkedList<>());
			this.taskRepository.completeTaskExecution(firstTaskExecutionId, 0, new Date(), "all done");
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, Collections.emptyMap(), new LinkedList<>());

			TaskManifest lastManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);

			assertEquals("file:src/test/resources/apps/foo-task", lastManifest.getTaskDeploymentRequest().getResource().getURL().toString());
			assertEquals("default", lastManifest.getPlatformName());
			assertEquals(1, lastManifest.getTaskDeploymentRequest().getDeploymentProperties().size());
			assertEquals("bar", lastManifest.getTaskDeploymentRequest().getDeploymentProperties().get("app.demo.foo"));

			verify(this.taskLauncher, never()).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		public void testRestoreDeployerPropertiesV2() throws IOException {
			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0", "1");

			Map<String, String> properties = new HashMap<>(1);
			properties.put("deployer.demo.memory", "100000GB");

			long firstTaskExecutionId = this.taskExecutionService.executeTask(TASK_NAME_ORIG, properties, new LinkedList<>());
			this.taskRepository.completeTaskExecution(firstTaskExecutionId, 0, new Date(), "all done");
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, Collections.emptyMap(), new LinkedList<>());

			TaskManifest lastManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);

			assertEquals("file:src/test/resources/apps/foo-task", lastManifest.getTaskDeploymentRequest().getResource().getURL().toString());
			assertEquals("default", lastManifest.getPlatformName());
			assertEquals(1, lastManifest.getTaskDeploymentRequest().getDeploymentProperties().size());
			assertEquals("100000GB", lastManifest.getTaskDeploymentRequest().getDeploymentProperties().get("spring.cloud.deployer.memory"));

			verify(this.taskLauncher, never()).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		public void testUpgradeDueToDeploymentPropsChangeForCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			this.launcherRepository.save(new Launcher("default", "Cloud Foundry", taskLauncher));
			setupUpgradeDueToDeploymentPropsChangeForCloudFoundry();
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		public void testUpgradeDueToDeploymentPropsChangeForCloudFoundryFailsWhenAlreadyRunning() throws IOException {
			this.launcherRepository.delete(this.launcher);
			this.launcherRepository.save(new Launcher("default", "Cloud Foundry", taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			TaskExecution myTask = this.taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			this.dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			this.taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, new Date(), new ArrayList<>(), null);
			this.taskRepository.updateExternalExecutionId(myTask.getExecutionId(), "abc");
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			when(this.taskLauncher.status("abc")).thenReturn(new TaskStatus("abc", LaunchState.running,new HashMap<>()));
			thrown.expect(IllegalStateException.class);
			thrown.expectMessage("Unable to update application due to currently running applications");
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
		}

		@Test
		@DirtiesContext
		public void testUpgradeDueToDeploymentPropsChangeForCloudFoundrySucceedsIfNotReallyRunning() throws IOException {
			this.launcherRepository.delete(this.launcher);
			this.launcherRepository.save(new Launcher("default", "Cloud Foundry", taskLauncher));
			initializeSuccessfulRegistry(appRegistry);
			TaskExecution myTask = this.taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);
			this.dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			this.taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, new Date(), new ArrayList<>(), null);
			this.taskRepository.updateExternalExecutionId(myTask.getExecutionId(), "abc");
			when(this.taskLauncher.launch(any())).thenReturn("abc");
			when(this.taskLauncher.status("abc")).thenReturn(new TaskStatus("abc", LaunchState.failed, new HashMap<>()));
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		public void testUpgradeDueToDeploymentPropsChangeForOther() throws IOException {
			setupUpgradeDueToDeploymentPropsChangeForCloudFoundry();
			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeDueToDeploymentPropsChangeForCloudFoundry() throws IOException {
			TaskExecution myTask = this.taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);

			this.dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			this.taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, new Date(), new ArrayList<>(), null);
			this.taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, new Date(), null);
			this.taskRepository.updateExternalExecutionId(myTask.getExecutionId(), "0");

			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String,String> deploymentProperties = new HashMap<>(1);
			deploymentProperties.put("deployer.demo.memory", "10000GB");

			this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, new LinkedList<>());

			TaskManifest lastManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);

			assertEquals("file:src/test/resources/apps/foo-task", lastManifest.getTaskDeploymentRequest().getResource().getURL().toString());
			assertEquals("default", lastManifest.getPlatformName());
			assertEquals(1, lastManifest.getTaskDeploymentRequest().getDeploymentProperties().size());
			assertEquals("10000GB", lastManifest.getTaskDeploymentRequest().getDeploymentProperties().get("spring.cloud.deployer.memory"));

		}

		@Test
		@DirtiesContext
		public void testUpgradeDueToAppPropsChangeCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			this.launcherRepository.save(new Launcher("default", "Cloud Foundry", taskLauncher));
			setupUpgradeForAppPropsChange();
			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}

		@Test
		@DirtiesContext
		public void testCommandLineArgChangeCloudFoundry() throws IOException {
			this.launcherRepository.delete(this.launcher);
			this.launcherRepository.save(new Launcher("default", "Cloud Foundry", taskLauncher));
			this.setupUpgradeForCommandLineArgsChange();

			verify(this.taskLauncher).destroy(TASK_NAME_ORIG);
		}
		@Test
		@DirtiesContext
		public void testCommandLineArgChangeOther() throws IOException {
			this.setupUpgradeForCommandLineArgsChange();

			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeForCommandLineArgsChange() throws IOException {
			 TaskExecution myTask = this.taskRepository.createTaskExecution(TASK_NAME_ORIG);
			 TaskManifest manifest = new TaskManifest();
			 manifest.setPlatformName("default");
			 AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					 new FileUrlResource("src/test/resources/apps/foo-task"));
			 manifest.setTaskDeploymentRequest(request);

			 this.dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			 this.taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, new Date(), new ArrayList<>(), null);
			 this.taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, new Date(), null);

			 initializeSuccessfulRegistry(appRegistry);

			 when(taskLauncher.launch(any())).thenReturn("0");

			 Map<String,String> deploymentProperties = new HashMap<>(1);

			 this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, Collections.singletonList("--foo=bar"));
			 TaskManifest lastManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);
			 assertEquals(2, lastManifest.getTaskDeploymentRequest().getCommandlineArguments().size());
			 assertEquals("--foo=bar", lastManifest.getTaskDeploymentRequest().getCommandlineArguments().get(0));

			 this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, Collections.emptyList());
			 lastManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);
			 assertEquals(1, lastManifest.getTaskDeploymentRequest().getCommandlineArguments().size());
		 }

		@Test
		@DirtiesContext
		public void testUpgradeDueToAppPropsChangeOther() throws IOException {
			setupUpgradeForAppPropsChange();
			verify(this.taskLauncher, times(0)).destroy(TASK_NAME_ORIG);
		}

		private void setupUpgradeForAppPropsChange() throws IOException {
			TaskExecution myTask = this.taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);

			this.dataflowTaskExecutionMetadataDao.save(myTask, manifest);
			this.taskRepository.startTaskExecution(myTask.getExecutionId(), TASK_NAME_ORIG, new Date(), new ArrayList<>(), null);
			this.taskRepository.completeTaskExecution(myTask.getExecutionId(), 0, new Date(), null);

			initializeSuccessfulRegistry(appRegistry);

			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String,String> deploymentProperties = new HashMap<>(1);
			deploymentProperties.put("app.demo.foo", "bar");

			this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, new LinkedList<>());

			TaskManifest lastManifest = this.dataflowTaskExecutionMetadataDao.getLatestManifest(TASK_NAME_ORIG);

			assertEquals("file:src/test/resources/apps/foo-task", lastManifest.getTaskDeploymentRequest().getResource().getURL().toString());
			assertEquals("default", lastManifest.getPlatformName());
			assertEquals(7, lastManifest.getTaskDeploymentRequest().getDefinition().getProperties().size());
			assertEquals("bar", lastManifest.getTaskDeploymentRequest().getDefinition().getProperties().get("foo"));
		}

		@Test(expected = IllegalStateException.class)
		@DirtiesContext
		public void testUpgradeFailureTaskCurrentlyRunning() throws MalformedURLException {
			TaskExecution myTask = this.taskRepository.createTaskExecution(TASK_NAME_ORIG);
			TaskManifest manifest = new TaskManifest();
			manifest.setPlatformName("default");
			AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("some-name", null),
					new FileUrlResource("src/test/resources/apps/foo-task"));
			manifest.setTaskDeploymentRequest(request);

			this.dataflowTaskExecutionMetadataDao.save(myTask, manifest);

			initializeSuccessfulRegistry(appRegistry);
			this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
		}
	}

	@TestPropertySource(properties = { "spring.cloud.dataflow.task.maximum-concurrent-tasks=10" })
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	public static class SimpleTaskTests extends DefaultTaskExecutionServiceTests {

		@Before
		public void setup() {
			this.launcherRepository.save(new Launcher("default", "local", taskLauncher));
			this.launcherRepository.save(new Launcher("MyPlatform", "local", taskLauncher));

			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		}

		@Test
		@DirtiesContext
		public void createSimpleTask() {
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			verifyTaskExistsInRepo("simpleTask", "AAA --foo=bar", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		public void executeSingleTaskTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));

			TaskDeployment taskDeployment = taskDeploymentRepository.findByTaskDeploymentId("0");
			assertNotNull("TaskDeployment should not be null", taskDeployment);
			assertEquals("0", taskDeployment.getTaskDeploymentId());
			assertEquals(TASK_NAME_ORIG, taskDeployment.getTaskDefinitionName());
			assertEquals("default", taskDeployment.getPlatformName());
			assertNotNull("TaskDeployment createdOn field should not be null", taskDeployment.getCreatedOn());
		}

		@Test
		@DirtiesContext
		public void executeStopTaskTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));

			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(1L);
			taskExecutionService.stopTaskExecution(executionIds);
			String logEntries = outputCapture.toString();
			assertTrue(logEntries.contains("Task execution stop request for id 1 for platform default has been submitted"));
		}

		@Test
		@DirtiesContext
		public void executeStopForSpecificPlatformTaskTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));

			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(1L);
			taskExecutionService.stopTaskExecution(executionIds, "MyPlatform");
			String logEntries = outputCapture.toString();
			assertTrue(logEntries.contains("Task execution stop request for id 1 for platform MyPlatform has been submitted"));
		}

		@Test
		@DirtiesContext
		public void executeStopTaskWithNoChildExternalIdTest() {
			initializeSuccessfulRegistry(this.appRegistry);
			when(this.taskLauncher.launch(any())).thenReturn("0");
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));
			TaskExecution taskExecution = this.taskRepository.createTaskExecution();
			this.taskRepository.startTaskExecution(taskExecution.getExecutionId(), "invalidChildTaskExecution", new Date(), Collections.emptyList(),null,1L);
			validateFailedTaskStop(2);
		}

		@Test
		@DirtiesContext
		public void executeStopTaskWithNoExternalIdTest() {
			this.taskRepository.createTaskExecution("invalidExternalTaskId");
			validateFailedTaskStop(1);
		}

		private void validateFailedTaskStop(long id) {
			boolean errorCaught = false;
			Set<Long> executionIds = new HashSet<>(1);
			executionIds.add(1L);
			try {
				this.taskExecutionService.stopTaskExecution(executionIds);

			} catch (TaskExecutionMissingExternalIdException ise) {
				errorCaught = true;
				assertEquals(String.format("The TaskExecutions with the following ids: %s do not have external execution ids.", id), ise.getMessage());
			}
			if (!errorCaught) {
				fail();
			}
		}

		@Test(expected = NoSuchTaskExecutionException.class)
		@DirtiesContext
		public void executeStopInvalidIdTaskTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));

			Set<Long> executionIds = new HashSet<>(2);
			executionIds.add(1L);
			executionIds.add(5L);
			taskExecutionService.stopTaskExecution(executionIds);
		}

		@Test
		@DirtiesContext
		public void executeMultipleTasksTest() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));
			assertEquals(2L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));
		}

		@Test
		@DirtiesContext
		public void getTaskLog() {
			String platformName = "test-platform";
			String taskDefinitionName = "test";
			String taskDeploymentId = "12345";
			TaskDeployment taskDeployment = new TaskDeployment();
			taskDeployment.setPlatformName(platformName);
			taskDeployment.setTaskDefinitionName(taskDefinitionName);
			taskDeployment.setTaskDeploymentId(taskDeploymentId);
			this.launcherRepository.save(new Launcher(platformName, "local", taskLauncher));
			when(taskLauncher.getLog(taskDeploymentId)).thenReturn("Logs");
			assertEquals("Logs", this.taskExecutionService.getLog(taskDeployment.getPlatformName(), taskDeploymentId));
		}

		@Test
		@DirtiesContext
		public void getCFTaskLog() {
			String platformName = "cf-test-platform";
			String taskDefinitionName = "test";
			String taskDeploymentId = "12345";
			TaskDeployment taskDeployment = new TaskDeployment();
			taskDeployment.setPlatformName(platformName);
			taskDeployment.setTaskDefinitionName(taskDefinitionName);
			taskDeployment.setTaskDeploymentId(taskDeploymentId);
			this.taskDeploymentRepository.save(taskDeployment);
			this.launcherRepository.save(new Launcher(platformName,
					TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			when(taskLauncher.getLog(taskDefinitionName)).thenReturn("Logs");
			assertEquals("Logs", this.taskExecutionService.getLog(taskDeployment.getPlatformName(), taskDeploymentId));
		}

		@Test
		@DirtiesContext
		public void getCFTaskLogByInvalidTaskId() {
			String platformName = "cf-test-platform";
			String taskDeploymentId = "12345";
			this.launcherRepository.save(new Launcher(platformName,
					TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			assertEquals("Log could not be retrieved as the task instance is not running by the ID: 12345",
					this.taskExecutionService.getLog(platformName, taskDeploymentId));
		}

		@Test
		@DirtiesContext
		public void getCFTaskLogByTaskIdOtherThanLatest() {
			String taskName = "test-task";
			String platformName = "cf-test-platform";
			String taskDeploymentId = "12345";
			TaskDeployment taskDeployment = new TaskDeployment();
			taskDeployment.setPlatformName(platformName);
			taskDeployment.setTaskDeploymentId(taskDeploymentId);
			taskDeployment.setTaskDefinitionName(taskName);
			this.taskDeploymentRepository.save(taskDeployment);
			TaskExecution taskExecution = new TaskExecution();
			taskExecution.setStartTime(new Date());
			taskExecution.setTaskName(taskName);
			taskExecution.setExternalExecutionId("12346");
			this.taskRepository.createTaskExecution(taskExecution);
			this.launcherRepository.save(new Launcher(platformName,
					TaskPlatformFactory.CLOUDFOUNDRY_PLATFORM_TYPE, taskLauncher));
			assertEquals("", this.taskExecutionService.getLog(platformName, taskDeploymentId));
		}


		@Test
		@DirtiesContext
		public void executeSameTaskDefinitionOnMultiplePlatforms() {
			initializeSuccessfulRegistry(appRegistry);
			when(taskLauncher.launch(any())).thenReturn("0");
			this.launcherRepository.save(new Launcher("anotherPlatform", "local", taskLauncher));
			assertEquals(1L, this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>()));
			Map<String, String> deploymentProperties = new HashMap<>();
			deploymentProperties.put(DefaultTaskExecutionService.TASK_PLATFORM_NAME, "anotherPlatform");
			boolean errorCaught = false;
			try {
				this.taskExecutionService.executeTask(TASK_NAME_ORIG, deploymentProperties, new LinkedList<>());
			} catch (IllegalStateException ise) {
				errorCaught = true;
				assertEquals("Task definition [myTask_ORIG] has already been deployed on platform [default].  Requested to deploy on platform [anotherPlatform].", ise.getMessage());
			}
			if (!errorCaught) {
				fail();
			}
		}

		@Test
		@DirtiesContext
		public void executeTaskWithNullIDReturnedTest() {
			initializeSuccessfulRegistry(appRegistry);
			boolean errorCaught = false;
			when(this.taskLauncher.launch(any())).thenReturn(null);
			try {
				taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			}
			catch (IllegalStateException ise) {
				errorCaught = true;
				assertEquals("Deployment ID is null for the task:myTask_ORIG", ise.getMessage());
			}
			if (!errorCaught) {
				fail();
			}
		}

		@Test
		@DirtiesContext
		public void executeTaskWithNullDefinitionTest() {
			boolean errorCaught = false;
			when(this.taskLauncher.launch(any())).thenReturn("0");
			TaskConfigurationProperties taskConfigurationProperties = new TaskConfigurationProperties();
			ComposedTaskRunnerConfigurationProperties composedTaskRunnerConfigurationProperties =
					new ComposedTaskRunnerConfigurationProperties();
			TaskExecutionInfoService taskExecutionInfoService = new DefaultTaskExecutionInfoService(
					this.dataSourceProperties, this.appRegistry, this.taskExplorer,
					mock(TaskDefinitionRepository.class), taskConfigurationProperties,
					mock(LauncherRepository.class), Collections.singletonList(mock(TaskPlatform.class)),
					composedTaskRunnerConfigurationProperties);
			TaskExecutionService taskExecutionService = new DefaultTaskExecutionService(
					launcherRepository, auditRecordService, taskRepository,
					taskExecutionInfoService, mock(TaskDeploymentRepository.class),
					taskExecutionRepositoryService, taskAppDeploymentRequestCreator,
					this.taskExplorer, this.dataflowTaskExecutionDao, this.dataflowTaskExecutionMetadataDao,
					mock(OAuth2TokenUtilsService.class), this.taskSaveService, taskConfigurationProperties,
					composedTaskRunnerConfigurationProperties);
			try {
				taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), new LinkedList<>());
			}
			catch (NoSuchTaskDefinitionException ise) {
				errorCaught = true;
				assertEquals("Could not find task definition named myTask_ORIG", ise.getMessage());
			}
			if (!errorCaught) {
				fail();
			}
		}

		@Test
		@DirtiesContext
		public void validateValidTaskTest() {
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertEquals("valid", validationStatus.getAppsStatuses().get("task:simpleTask"));
		}

		@Test(expected = NoSuchTaskDefinitionException.class)
		@DirtiesContext
		public void validateMissingTaskDefinitionTest() {
			initializeSuccessfulRegistry(appRegistry);
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertEquals("valid", validationStatus.getAppsStatuses().get("task:simpleTask"));
		}

		@Test
		@DirtiesContext
		public void validateInvalidTaskTest() {
			initializeFailRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertEquals("invalid", validationStatus.getAppsStatuses().get("task:simpleTask"));
		}

		@Test
		@DirtiesContext
		public void validateInvalidTaskNameTest() {
			String[] taskNames = { "$task", "task$", "ta_sk" };

			for (String taskName : taskNames) {
				try {
					initializeSuccessfulRegistry(appRegistry);
					taskSaveService.saveTaskDefinition(new TaskDefinition(taskName, "AAA --foo=bar"));

					fail("Expected TaskException");
				} catch (Exception e) {
					assertTrue(e instanceof TaskException);
					assertEquals(e.getMessage(), "Task name must consist of alphanumeric characters or '-', start " +
							"with an alphabetic character, and end with an alphanumeric character (e.g. 'my-name', " +
							" or 'abc-123')");
				}
			}
		}

		@Test
		@DirtiesContext
		public void validateNullResourceTaskTest() {
			initializeNullRegistry(appRegistry);
			taskSaveService.saveTaskDefinition(new TaskDefinition("simpleTask", "AAA --foo=bar"));
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertEquals("invalid", validationStatus.getAppsStatuses().get("task:simpleTask"));
		}
	}

	@TestPropertySource(properties = { "spring.cloud.dataflow.task.auto-create-task-definitions=true" })
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	public static class AutoCreateTaskDefinitionTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		TaskDefinitionRepository taskDefinitionRepository;

		@Autowired
		TaskExecutionInfoService taskExecutionInfoService;

		@Before
		public void setup() {
			this.launcherRepository.save(new Launcher("default", "local", taskLauncher));
		}

		@Test
		@DirtiesContext
		public void executeTaskWithNullDefinitionCreatesDefinitionIfConfigured() {
			initializeSuccessfulRegistry(appRegistry);
			when(this.taskLauncher.launch(any())).thenReturn("0");
			taskExecutionService.executeTask("demo", new HashMap<>(), new LinkedList<>());
			assertNotNull(taskDefinitionRepository.findByTaskName("demo"));
		}
	}


	@TestPropertySource(properties = { "spring.cloud.dataflow.applicationProperties.task.globalkey=globalvalue",
			"spring.cloud.dataflow.applicationProperties.stream.globalstreamkey=nothere" })
	@AutoConfigureTestDatabase(replace = Replace.ANY)

	public static class ComposedTaskTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		TaskRepository taskExecutionRepository;

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

		@Before
		public void setupMocks() {
			this.launcherRepository.save(new Launcher("default", "local", taskLauncher));
			this.launcherRepository.save(new Launcher("MyPlatform", "local", taskLauncher));
		}

		@Test
		@DirtiesContext
		public void executeComposedTask() {
			AppDeploymentRequest request = prepComposedTaskRunner(null);
			assertEquals("seqTask", request.getDefinition().getProperties().get("spring.cloud.task.name"));
			assertTrue(request.getDefinition().getProperties().containsKey("composed-task-properties"));
			assertEquals(
					"app.seqTask-AAA.app.AAA.timestamp.format=YYYY, deployer.seqTask-AAA.deployer.AAA.memory=1240m",
					request.getDefinition().getProperties().get("composed-task-properties"));
			assertTrue(request.getDefinition().getProperties().containsKey("interval-time-between-checks"));
			assertEquals("1000", request.getDefinition().getProperties().get("interval-time-between-checks"));
			assertFalse(request.getDefinition().getProperties().containsKey("app.foo"));
			assertEquals("globalvalue", request.getDefinition().getProperties().get("globalkey"));
			assertNull(request.getDefinition().getProperties().get("globalstreamkey"));
			assertEquals("default", request.getDefinition().getProperties().get("platform-name"));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskNewPlatform() {
			AppDeploymentRequest request = prepComposedTaskRunner("MyPlatform");
			assertEquals("seqTask", request.getDefinition().getProperties().get("spring.cloud.task.name"));
			assertTrue(request.getDefinition().getProperties().containsKey("composed-task-properties"));
			assertEquals(
					"app.seqTask-AAA.app.AAA.timestamp.format=YYYY, deployer.seqTask-AAA.deployer.AAA.memory=1240m",
					request.getDefinition().getProperties().get("composed-task-properties"));
			assertTrue(request.getDefinition().getProperties().containsKey("interval-time-between-checks"));
			assertEquals("1000", request.getDefinition().getProperties().get("interval-time-between-checks"));
			assertFalse(request.getDefinition().getProperties().containsKey("app.foo"));
			assertEquals("globalvalue", request.getDefinition().getProperties().get("globalkey"));
			assertNull(request.getDefinition().getProperties().get("globalstreamkey"));
			assertEquals("MyPlatform", request.getDefinition().getProperties().get("platform-name"));
		}

		private AppDeploymentRequest prepComposedTaskRunner(String platformName) {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");
			when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);
			Map<String, String> properties = new HashMap<>();
			if(StringUtils.hasText(platformName)) {
				properties.put("spring.cloud.dataflow.task.platformName", platformName);
			}
			properties.put("app.foo", "bar");
			properties.put("app.seqTask.AAA.timestamp.format", "YYYY");
			properties.put("deployer.seqTask.AAA.memory", "1240m");
			properties.put("app.composed-task-runner.interval-time-between-checks", "1000");
			assertEquals(1L, this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>()));
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());
			return argumentCaptor.getValue();
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithAccessTokenDisabled1() {
			initializeSuccessfulRegistry(appRegistry);
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService,
					this.taskLauncher, this.appRegistry), Collections.emptyList(),
					this.taskExecutionService, this.taskLauncher);
			assertFalse("Should not contain the property 'dataflow-server-access-token'",
				request.getDefinition().getProperties().containsKey("dataflow-server-access-token"));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithAccessTokenDisabled2() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			arguments.add("--dataflow-server-use-user-access-token=false");
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService,
					this.taskLauncher, this.appRegistry), Collections.emptyList(),
					this.taskExecutionService, this.taskLauncher);
			assertFalse("Should not contain the property 'dataflow-server-access-token'",
				request.getDefinition().getProperties().containsKey("dataflow-server-access-token"));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithEnabledUserAccessToken1() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			arguments.add("--dataflow-server-use-user-access-token=true");
			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService,
					this.taskLauncher, this.appRegistry), arguments,  this.taskExecutionService,
					this.taskLauncher);
			assertTrue("Should contain the property 'dataflow-server-access-token'",
				request.getDefinition().getProperties().containsKey("dataflow-server-access-token"));
			assertEquals("foo-bar-123-token", request.getDefinition().getProperties().get("dataflow-server-access-token"));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithEnabledUserAccessToken2() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			arguments.add("--dataflow-server-use-user-access-token =  true");
			AppDeploymentRequest request = getAppDeploymentRequestForToken(
					prepareEnvironmentForTokenTests(this.taskSaveService,
					this.taskLauncher, this.appRegistry), arguments,
					this.taskExecutionService, this.taskLauncher);
			assertTrue("Should contain the property 'dataflow-server-access-token'",
				request.getDefinition().getProperties().containsKey("dataflow-server-access-token"));
			assertEquals("foo-bar-123-token", request.getDefinition().getProperties().get("dataflow-server-access-token"));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithAccessTokenOverrideAsProperty() {
			initializeSuccessfulRegistry(appRegistry);

			Map<String, String> properties = prepareEnvironmentForTokenTests(this.taskSaveService,
					this.taskLauncher, this.appRegistry);
			properties.put("app.composed-task-runner.dataflow-server-access-token", "foo-bar-123-token-override");

			AppDeploymentRequest request = getAppDeploymentRequestForToken(properties, Collections.emptyList(),
					this.taskExecutionService, this.taskLauncher);

			assertTrue("Should contain the property 'dataflow-server-access-token'",
				request.getDefinition().getProperties().containsKey("dataflow-server-access-token"));

			boolean containsArgument = false;
			for (String argument : request.getCommandlineArguments()) {
				if (argument.contains("--dataflow-server-access-token")) {
					containsArgument = true;
				}
			}

			assertFalse("Should not contain the argument 'dataflow-server-access-token'", containsArgument);
			assertEquals("foo-bar-123-token-override", request.getDefinition().getProperties().get("dataflow-server-access-token"));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithAccessTokenOverrideAsArgument() {
			initializeSuccessfulRegistry(appRegistry);

			List<String> args = Collections.singletonList("--dataflow-server-access-token=foo-bar-123-token-override");

			AppDeploymentRequest request = getAppDeploymentRequestForToken(prepareEnvironmentForTokenTests(this.taskSaveService,
					this.taskLauncher, this.appRegistry), args,  this.taskExecutionService,
					this.taskLauncher);

			assertFalse("Should not contain the property 'dataflow-server-access-token'",
				request.getDefinition().getProperties().containsKey("dataflow-server-access-token"));

			boolean containsArgument = false;
			String argumentValue = null;
			for (String argument : request.getCommandlineArguments()) {
				if (argument.contains("--dataflow-server-access-token")) {
					containsArgument = true;
					argumentValue = argument;
				}
			}
			assertFalse("Should not contain the property 'dataflow-server-access-token'",
					request.getCommandlineArguments().contains("dataflow-server-access-token"));
			assertTrue("Should contain the argument 'dataflow-server-access-token'", containsArgument);
			assertEquals("--dataflow-server-access-token=foo-bar-123-token-override", argumentValue);
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskwithUserCTRName() {
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
			assertEquals(1L, this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>()));
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

			AppDeploymentRequest request = argumentCaptor.getValue();
			assertEquals("seqTask", request.getDefinition().getProperties().get("spring.cloud.task.name"));
			assertTrue(request.getDefinition().getProperties().containsKey("composed-task-properties"));
			assertEquals(request.getCommandlineArguments().get(1),"--spring.cloud.data.flow.taskappname=composed-task-runner");
			assertEquals(
					"app.seqTask-AAA.app.AAA.timestamp.format=YYYY, deployer.seqTask-AAA.deployer.AAA.memory=1240m",
					request.getDefinition().getProperties().get("composed-task-properties"));
			assertTrue(request.getDefinition().getProperties().containsKey("interval-time-between-checks"));
			assertEquals("1000", request.getDefinition().getProperties().get("interval-time-between-checks"));
			assertFalse(request.getDefinition().getProperties().containsKey("app.foo"));
			assertEquals("globalvalue", request.getDefinition().getProperties().get("globalkey"));
			assertNull(request.getDefinition().getProperties().get("globalstreamkey"));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithLabels() {
			String dsl = "t1: AAA && t2: BBB";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.launch(any())).thenReturn("0");

			Map<String, String> properties = new HashMap<>();
			properties.put("app.seqTask.t1.timestamp.format", "YYYY");
			properties.put("app.composed-task-runner.interval-time-between-checks", "1000");
			assertEquals(1L, this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>()));
			ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
			verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

			AppDeploymentRequest request = argumentCaptor.getValue();
			assertEquals("seqTask", request.getDefinition().getProperties().get("spring.cloud.task.name"));
			assertTrue(request.getDefinition().getProperties().containsKey("composed-task-properties"));
			assertEquals("app.seqTask-t1.app.AAA.timestamp.format=YYYY",
					request.getDefinition().getProperties().get("composed-task-properties"));
			assertTrue(request.getDefinition().getProperties().containsKey("interval-time-between-checks"));
			assertEquals("1000", request.getDefinition().getProperties().get("interval-time-between-checks"));
		}

		@Test
		@DirtiesContext
		public void createSequenceComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA && BBB";
			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			verifyTaskExistsInRepo("seqTask", dsl, taskDefinitionRepository);

			verifyTaskExistsInRepo("seqTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("seqTask-BBB", "BBB", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		public void createSplitComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "<AAA || BBB>";
			taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			verifyTaskExistsInRepo("splitTask", dsl, taskDefinitionRepository);

			verifyTaskExistsInRepo("splitTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("splitTask-BBB", "BBB", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		public void verifyComposedTaskFlag() {
			String composedTaskDsl = "<AAA || BBB>";
			assertTrue("Expected true for composed task", TaskServiceUtils.isComposedTaskDefinition(composedTaskDsl));
			composedTaskDsl = "AAA 'FAILED' -> BBB '*' -> CCC";
			assertTrue("Expected true for composed task", TaskServiceUtils.isComposedTaskDefinition(composedTaskDsl));
			composedTaskDsl = "AAA && BBB && CCC";
			assertTrue("Expected true for composed task", TaskServiceUtils.isComposedTaskDefinition(composedTaskDsl));
			String nonComposedTaskDsl = "AAA";
			assertFalse("Expected false for non-composed task",
					TaskServiceUtils.isComposedTaskDefinition(nonComposedTaskDsl));
			nonComposedTaskDsl = "AAA --foo=bar";
			assertFalse("Expected false for non-composed task",
					TaskServiceUtils.isComposedTaskDefinition(nonComposedTaskDsl));
		}

		@Test
		@DirtiesContext
		public void verifyComposedTaskConcurrentCountExceeded() {
			String dsl = "<AAA || BBB>";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask", dsl));
			when(taskLauncher.getMaximumConcurrentTasks()).thenReturn(20);
			when(taskLauncher.launch(any())).thenReturn("0");
			when(appRegistry.appExist(anyString(), any(ApplicationType.class))).thenReturn(true);
			Map<String, String> properties = new HashMap<>();
			assertEquals(1L, this.taskExecutionService.executeTask("seqTask", properties, new LinkedList<>()));

			initializeSuccessfulRegistry(appRegistry);
			dsl = "<AAA||BBB||CCC>&&<AAA1||BBB1||CCC1||DDD1 --foo=bar||DDD2||DDD3||DDD4||DDD5||DDD6||" +
					"DDD7||DDD8||DDD9||DDD10||DDD11||DDD12||DDD13||DDD14||DDD15||DDD16||" +
					"DDD17||DDD18>&&<AAA2||BBB2>";
			taskSaveService.saveTaskDefinition(new TaskDefinition("seqTask1", dsl));
			try {
				this.taskExecutionService.executeTask("seqTask1", properties, new LinkedList<>());
			}
			catch (IllegalArgumentException iae) {
				assertEquals("One or more of the splits in the composed task contains " +
						"a task count that exceeds the maximumConcurrentTasks count of 20",
						iae.getMessage());
				return;
			}
			fail("Expected IllegalArgumentException maxConcurrentTasks exceeded was not thrown");
		}

		@Test
		@DirtiesContext
		public void createTransitionComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA 'FAILED' -> BBB '*' -> CCC";
			taskSaveService.saveTaskDefinition(new TaskDefinition("transitionTask", dsl));
			verifyTaskExistsInRepo("transitionTask", dsl, taskDefinitionRepository);

			verifyTaskExistsInRepo("transitionTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("transitionTask-BBB", "BBB", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		public void deleteAllComposedTask() {
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
			assertThat(preDeleteSize - 5, is(equalTo(taskDefinitionRepository.count())));
		}

		@Test
		@DirtiesContext
		public void deleteComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA && BBB && CCC";
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask", dsl));
			verifyTaskExistsInRepo("deleteTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-BBB", "BBB", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-CCC", "CCC", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask", dsl, taskDefinitionRepository);

			long preDeleteSize = taskDefinitionRepository.count();
			taskDeleteService.deleteTaskDefinition("deleteTask");
			assertThat(preDeleteSize - 4, is(equalTo(taskDefinitionRepository.count())));
		}

		@Test
		@DirtiesContext
		public void deleteComposedTaskMissingChildTasks() {
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
			assertThat(preDeleteSize - 3, is(equalTo(taskDefinitionRepository.count())));
		}

		@Test
		@DirtiesContext
		public void deleteComposedTaskDeleteOnlyChildren() {
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
			assertThat(preDeleteSize - 3, is(equalTo(taskDefinitionRepository.count())));
			verifyTaskExistsInRepo("deleteTask-AAA", "AAA", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		public void deleteComposedTaskWithLabel() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "LLL: AAA && BBB";
			taskSaveService.saveTaskDefinition(new TaskDefinition("deleteTask", dsl));
			verifyTaskExistsInRepo("deleteTask-LLL", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask-BBB", "BBB", taskDefinitionRepository);
			verifyTaskExistsInRepo("deleteTask", dsl, taskDefinitionRepository);

			long preDeleteSize = taskDefinitionRepository.count();
			taskDeleteService.deleteTaskDefinition("deleteTask");
			assertThat(preDeleteSize - 3, is(equalTo(taskDefinitionRepository.count())));
		}

		@Test
		@DirtiesContext
		public void createFailedComposedTask() {
			String dsl = "AAA && BBB";
			initializeFailRegistry(appRegistry);
			boolean isExceptionThrown = false;
			try {
				taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			}
			catch (IllegalArgumentException iae) {
				isExceptionThrown = true;
			}
			assertTrue("IllegalArgumentException was expected to be thrown", isExceptionThrown);
			assertFalse(wasTaskDefinitionCreated("splitTask", taskDefinitionRepository));
			assertFalse(wasTaskDefinitionCreated("splitTask-AAA", taskDefinitionRepository));
			assertFalse(wasTaskDefinitionCreated("splitTask-BBB", taskDefinitionRepository));
		}

		@Test
		@DirtiesContext
		public void createDuplicateComposedTask() {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);
			boolean isExceptionThrown = false;
			taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			try {
				taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			}
			catch (DuplicateTaskException de) {
				isExceptionThrown = true;
			}
			assertTrue("DuplicateTaskException was expected to be thrown", isExceptionThrown);
			assertTrue(wasTaskDefinitionCreated("splitTask", taskDefinitionRepository));
			assertTrue(wasTaskDefinitionCreated("splitTask-AAA", taskDefinitionRepository));
			assertTrue(wasTaskDefinitionCreated("splitTask-BBB", taskDefinitionRepository));
		}

		@Test
		@DirtiesContext
		public void createDuplicateChildTaskComposedTask() {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);
			boolean isExceptionThrown = false;
			taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask-BBB", "BBB"));
			try {
				taskSaveService.saveTaskDefinition(new TaskDefinition("splitTask", dsl));
			}
			catch (DuplicateTaskException de) {
				isExceptionThrown = true;
			}
			assertTrue("DuplicateTaskException was expected to be thrown", isExceptionThrown);
			assertFalse(wasTaskDefinitionCreated("splitTask", taskDefinitionRepository));
			assertFalse(wasTaskDefinitionCreated("splitTask-AAA", taskDefinitionRepository));
			assertTrue(wasTaskDefinitionCreated("splitTask-BBB", taskDefinitionRepository));
		}
	}

	@TestPropertySource(properties = { "spring.cloud.dataflow.applicationProperties.task.globalkey=globalvalue",
			"spring.cloud.dataflow.applicationProperties.stream.globalstreamkey=nothere",
			"spring.cloud.dataflow.task.useUserAccessToken=true"})
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	public static class ComposedTaskWithSystemUseUserAccessTokenTests extends DefaultTaskExecutionServiceTests {

		@Autowired
		TaskRepository taskExecutionRepository;

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

		@Before
		public void setupMocks() {
			this.launcherRepository.save(new Launcher("default", "local", taskLauncher));
			this.launcherRepository.save(new Launcher("MyPlatform", "local", taskLauncher));
		}

		@Test
		@DirtiesContext
		public void executeComposedTaskWithEnabledUserAccessToken1() {
			initializeSuccessfulRegistry(appRegistry);

			final List<String> arguments = new ArrayList<>();
			AppDeploymentRequest request = getAppDeploymentRequestForToken(
					prepareEnvironmentForTokenTests(this.taskSaveService,
							this.taskLauncher, this.appRegistry), arguments, this.taskExecutionService,
							this.taskLauncher);
			assertTrue("Should contain the property 'dataflow-server-access-token'",
					request.getDefinition().getProperties().containsKey("dataflow-server-access-token"));
			assertEquals("foo-bar-123-token", request.getDefinition().getProperties().get("dataflow-server-access-token"));
		}
	}

	static AppDeploymentRequest getAppDeploymentRequestForToken(Map<String, String> taskDeploymentProperties,
			List<String> commandLineArgs, TaskExecutionService taskExecutionService,
			TaskLauncher taskLauncher) {
		assertEquals(1L, taskExecutionService.executeTask("seqTask", taskDeploymentProperties, commandLineArgs));

		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

		final AppDeploymentRequest request = argumentCaptor.getValue();

		return request;
	}

	static Map<String, String> prepareEnvironmentForTokenTests(TaskSaveService taskSaveService,
			TaskLauncher taskLauncher, AppRegistryService appRegistry) {
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
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		try {
			when(appRegistry.getAppResource(any())).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task"));
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid File Resource Specified", e);
		}
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}

	private static void initializeFailRegistry(AppRegistryService appRegistry) throws IllegalArgumentException {
		when(appRegistry.find("BBB", ApplicationType.task)).thenThrow(new IllegalArgumentException(
				String.format("Application name '%s' with type '%s' does not exist in the app registry.", "fake",
						ApplicationType.task)));
		when(appRegistry.find("AAA", ApplicationType.task)).thenReturn(new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task/bad.jar"));
	}

	private static void initializeNullRegistry(AppRegistryService appRegistry) throws IllegalArgumentException {
		when(appRegistry.find("BBB", ApplicationType.task)).thenThrow(new IllegalArgumentException(
				String.format("Application name '%s' with type '%s' does not exist in the app registry.", "fake",
						ApplicationType.task)));
		when(appRegistry.find("AAA", ApplicationType.task)).thenReturn(mock(AppRegistration.class));
	}

	private static void verifyTaskExistsInRepo(String taskName, String dsl,
			TaskDefinitionRepository taskDefinitionRepository) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findById(taskName).get();
		assertThat(taskDefinition.getName(), is(equalTo(taskName)));
		assertThat(taskDefinition.getDslText(), is(equalTo(dsl)));
	}

	private static boolean wasTaskDefinitionCreated(String taskName,
			TaskDefinitionRepository taskDefinitionRepository) {
		return taskDefinitionRepository.findById(taskName).isPresent();
	}
}
