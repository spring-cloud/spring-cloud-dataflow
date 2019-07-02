/*
 * Copyright 2015-2019 the original author or authors.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.DataflowTaskExecutionDao;
import org.springframework.cloud.dataflow.server.repository.DuplicateTaskException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskExecutionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionCreationService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.dataflow.server.service.TaskValidationService;
import org.springframework.cloud.dataflow.server.service.ValidationStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Daniel Serleg
 * @author David Turanski
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
public abstract class DefaultTaskExecutionServiceTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

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
	TaskExecutionInfoService taskExecutionInfoService;

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

	@TestPropertySource(properties = { "spring.cloud.dataflow.task.maximum-concurrent-tasks=10" })
	@AutoConfigureTestDatabase(replace = Replace.ANY)
	public static class SimpleTaskTests extends DefaultTaskExecutionServiceTests {

		@Before
		public void setupMocks() {
			this.launcherRepository.save(new Launcher("default", "local", taskLauncher));
			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
			taskDefinitionRepository.findAll();
		}

		@Test
		@DirtiesContext
		public void createSimpleTask() {
			initializeSuccessfulRegistry(appRegistry);
			taskSaveService.saveTaskDefinition("simpleTask", "AAA --foo=bar");
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
			assertTrue(logEntries.contains("Task execution stop request for id 1 has been submitted"));
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
			TaskExecutionInfoService taskExecutionInfoService = new DefaultTaskExecutionInfoService(
					this.dataSourceProperties, this.appRegistry, this.taskExplorer,
					mock(TaskDefinitionRepository.class), new TaskConfigurationProperties(),
					mock(LauncherRepository.class), Collections.singletonList(mock(TaskPlatform.class)));
			TaskExecutionService taskExecutionService = new DefaultTaskExecutionService(
					launcherRepository, auditRecordService, taskRepository,
					taskExecutionInfoService, mock(TaskDeploymentRepository.class),
					taskExecutionRepositoryService, taskAppDeploymentRequestCreator,
					this.taskExplorer, this.dataflowTaskExecutionDao);
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
			taskSaveService.saveTaskDefinition("simpleTask", "AAA --foo=bar");
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertEquals("valid", validationStatus.getAppsStatuses().get("task:simpleTask"));
		}

		@Test
		@DirtiesContext
		public void validateInvalidTaskTest() {
			initializeFailRegistry(appRegistry);
			taskSaveService.saveTaskDefinition("simpleTask", "AAA --foo=bar");
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertEquals("invalid", validationStatus.getAppsStatuses().get("task:simpleTask"));
		}

		@Test
		@DirtiesContext
		public void validateNullResourceTaskTest() {
			initializeNullRegistry(appRegistry);
			taskSaveService.saveTaskDefinition("simpleTask", "AAA --foo=bar");
			ValidationStatus validationStatus = taskValidationService.validateTask("simpleTask");
			assertEquals("invalid", validationStatus.getAppsStatuses().get("task:simpleTask"));
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
		}

		@Test
		@DirtiesContext
		public void executeComposedTask() {
			String dsl = "AAA && BBB";
			initializeSuccessfulRegistry(appRegistry);

			taskSaveService.saveTaskDefinition("seqTask", dsl);
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

			taskSaveService.saveTaskDefinition("seqTask", dsl);
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
			taskSaveService.saveTaskDefinition("seqTask", dsl);
			verifyTaskExistsInRepo("seqTask", dsl, taskDefinitionRepository);

			verifyTaskExistsInRepo("seqTask-AAA", "AAA", taskDefinitionRepository);
			verifyTaskExistsInRepo("seqTask-BBB", "BBB", taskDefinitionRepository);
		}

		@Test
		@DirtiesContext
		public void createSplitComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "<AAA || BBB>";
			taskSaveService.saveTaskDefinition("splitTask", dsl);
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
		public void createTransitionComposedTask() {
			initializeSuccessfulRegistry(appRegistry);
			String dsl = "AAA 'FAILED' -> BBB '*' -> CCC";
			taskSaveService.saveTaskDefinition("transitionTask", dsl);
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
			taskSaveService.saveTaskDefinition("deleteTask1", taskDsl1);
			taskSaveService.saveTaskDefinition("deleteTask2", taskDsl2);
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
			taskSaveService.saveTaskDefinition("deleteTask", dsl);
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
			taskSaveService.saveTaskDefinition("deleteTask", dsl);
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
			taskSaveService.saveTaskDefinition("deleteTask-AAA", "AAA");
			String dsl = "BBB && CCC";
			taskSaveService.saveTaskDefinition("deleteTask", dsl);
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
			taskSaveService.saveTaskDefinition("deleteTask", dsl);
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
				taskSaveService.saveTaskDefinition("splitTask", dsl);
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
			taskSaveService.saveTaskDefinition("splitTask", dsl);
			try {
				taskSaveService.saveTaskDefinition("splitTask", dsl);
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
			taskSaveService.saveTaskDefinition("splitTask-BBB", "BBB");
			try {
				taskSaveService.saveTaskDefinition("splitTask", dsl);
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

	private static void initializeSuccessfulRegistry(AppRegistryService appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task"));
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
