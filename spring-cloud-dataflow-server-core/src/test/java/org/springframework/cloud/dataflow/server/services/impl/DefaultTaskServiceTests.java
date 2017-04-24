/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.services.impl;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.configuration.metadata.ApplicationConfigurationMetadataResolver;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.AppRegistration;
import org.springframework.cloud.dataflow.registry.AppRegistry;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.repository.InMemoryDeploymentIdRepository;
import org.springframework.cloud.dataflow.server.repository.NoSuchTaskDefinitionException;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskService;
import org.springframework.cloud.dataflow.server.service.impl.DefaultTaskService;
import org.springframework.cloud.dataflow.server.service.impl.TaskConfigurationProperties;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.dataflow.core.ApplicationType.task;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class,
		TaskServiceDependencies.class})
public class DefaultTaskServiceTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	private final static String TASK_NAME_EXEC_ONE = BASE_TASK_NAME + "_ONE";

	private final static String TASK_NAME_EXEC_TWO = BASE_TASK_NAME + "_TWO";

	private final static String TASK_NAME_EXEC_THREE = BASE_TASK_NAME + "_THREE";

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	TaskRepository taskExecutionRepository;

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Autowired
	private TaskExplorer taskExplorer;

	@Autowired
	private DataSource dataSource;

	private AppRegistry appRegistry;

	private ResourceLoader resourceLoader;

	private TaskLauncher taskLauncher;

	private ApplicationConfigurationMetadataResolver metadataResolver;

	private TaskService taskService;

	private JdbcTemplate template;

	private List sampleArgumentList;

	private List sampleCleansedArgumentList;

	private boolean initialized = false;

	private Map<Long, TaskExecution> taskExecutionsMap;

	private Pageable defaultPageable;


	@Before
	public void setupMockMVC() {
		this.template = new JdbcTemplate(dataSource);
		this.template.execute("DELETE FROM task_execution");

		taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		appRegistry = mock(AppRegistry.class);
		resourceLoader = mock(ResourceLoader.class);
		metadataResolver = mock(ApplicationConfigurationMetadataResolver.class);
		taskLauncher = mock(TaskLauncher.class);
		when(this.appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
			new AppRegistration("some-name", task, URI.create("http://helloworld"), resourceLoader));
		when(this.resourceLoader.getResource(anyString())).
				thenReturn(mock(Resource.class));
		taskService =
				new DefaultTaskService(dataSourceProperties,
						taskDefinitionRepository, taskExplorer,
						taskExecutionRepository, appRegistry, resourceLoader,
						taskLauncher, metadataResolver,
						new TaskConfigurationProperties(),
						new InMemoryDeploymentIdRepository(), null);

		if(!this.initialized) {
			this.initialized = true;
			this.sampleArgumentList = new LinkedList<String>();
			this.sampleArgumentList.add("--password=foo");
			this.sampleArgumentList.add("password=bar");
			this.sampleArgumentList.add("org.woot.password=baz");
			this.sampleArgumentList.add("foo.bar=foo");
			this.sampleArgumentList.add("bar.baz = boo");
			this.sampleArgumentList.add("foo.credentials.boo=bar");
			this.sampleArgumentList.add("spring.datasource.username=dbuser");
			this.sampleArgumentList.add("spring.datasource.password=dbpass");

			this.sampleCleansedArgumentList = new LinkedList<String>();
			this.sampleCleansedArgumentList.add("--password=******");
			this.sampleCleansedArgumentList.add("password=******");
			this.sampleCleansedArgumentList.add("org.woot.password=******");
			this.sampleCleansedArgumentList.add("foo.bar=foo");
			this.sampleCleansedArgumentList.add("bar.baz = boo");
			this.sampleCleansedArgumentList.add("foo.credentials.boo=******");
			this.sampleCleansedArgumentList.add("spring.datasource.username=dbuser");
			this.sampleCleansedArgumentList.add("spring.datasource.password=******");

			this.defaultPageable = new PageRequest(0, 10);
			this.taskExecutionsMap = new HashMap<>();
			this.taskExecutionsMap.put(1L,  new TaskExecution(1L,
					0, TASK_NAME_EXEC_ONE, new Date(), null, null,
					new LinkedList<>(), null, "EXTERNAL_EXEC_ID", 45L));

			this.taskExecutionsMap.put(2L,  new TaskExecution(2L,
					0, TASK_NAME_EXEC_TWO, new Date(), null, null,
					this.sampleArgumentList, null, "EXTERNAL_EXEC_ID", 45L));

			this.taskExecutionsMap.put(3L,  new TaskExecution(3L,
					0, TASK_NAME_EXEC_THREE, new Date(), null, null,
					new LinkedList<>(), null, "EXTERNAL_EXEC_ID", 45L));
			
			this.taskExecutionsMap = Collections.unmodifiableMap(this.taskExecutionsMap);
		}

		this.taskExecutionRepository.createTaskExecution(this.taskExecutionsMap.get(1L));
		this.taskExecutionRepository.createTaskExecution(this.taskExecutionsMap.get(2L));
		this.taskExecutionRepository.createTaskExecution(this.taskExecutionsMap.get(3L));
	}

	@Test
	@DirtiesContext
	public void executeSingleTaskTest() {
		when(taskLauncher.launch(anyObject())).thenReturn("0");
		assertEquals(4L, this.taskService.executeTask(TASK_NAME_ORIG,
				new HashMap<>(), new LinkedList<>()));
	}

	@Test
	@DirtiesContext
	public void executeMultipleTasksTest() {
		when(taskLauncher.launch(anyObject())).thenReturn("0");
		assertEquals(4L, this.taskService.executeTask(TASK_NAME_ORIG,
				new HashMap<>(), new LinkedList<>()));
		assertEquals(5L, this.taskService.executeTask(TASK_NAME_ORIG,
				new HashMap<>(), new LinkedList<>()));
	}

	@Test
	@DirtiesContext
	public void viewTaskExecutionWithArgs() {
		final long EXECUTION_ID = 2L;
		TaskExecution actual = this.taskService.viewTaskExecution(EXECUTION_ID);
		verifyTaskExecution(this.taskExecutionsMap.get(EXECUTION_ID), actual,
				this.sampleCleansedArgumentList);
	}

	@Test
	@DirtiesContext
	public void viewTaskExecutionWithEmptyArg() {
		final long EXECUTION_ID = 1L;
		TaskExecution actual = this.taskService.viewTaskExecution(EXECUTION_ID);
		verifyTaskExecution(this.taskExecutionsMap.get(EXECUTION_ID), actual,
				new LinkedList<>());
	}

	@Test
	@DirtiesContext
	public void findAllTests() {
		Page<TaskExecution> taskExecutions =
				this.taskService.findAll(this.defaultPageable);
		Iterator<TaskExecution> taskExecutionIterator = taskExecutions.iterator();
		while(taskExecutionIterator.hasNext()) {
			TaskExecution actual = taskExecutionIterator.next();
			verifyTaskExecution(this.taskExecutionsMap.get(actual.getExecutionId()),
					actual, (actual.getExecutionId() == 2L) ?
							this.sampleCleansedArgumentList : new LinkedList<>());
		}
	}

	@Test
	@DirtiesContext
	public void findTaskExecutionsByNameTests() {
		verifyFindByName(TASK_NAME_EXEC_ONE, 1L, new LinkedList<>());
		verifyFindByName(TASK_NAME_EXEC_TWO, 2L, this.sampleCleansedArgumentList);
		verifyFindByName(TASK_NAME_EXEC_THREE, 3L, new LinkedList<>());
	}

	private void verifyFindByName(String taskName, long expectedExecutionId, List<String> expectedArgList) {
		Page<TaskExecution> taskExecutions =
				this.taskService.findTaskExecutionsByName(taskName,
						this.defaultPageable);
		List<TaskExecution> taskExecutionList = taskExecutions.getContent();
		assertThat(taskExecutionList.size(), is(1));
		verifyTaskExecution(this.taskExecutionsMap.get(expectedExecutionId),
				taskExecutionList.get(0), expectedArgList);
	}

	private void verifyTaskExecution(TaskExecution expected,
			TaskExecution actual, List<String> expectedArgList) {
		assertThat(actual, notNullValue());
		assertThat(expected, notNullValue());
		assertThat(actual.getExecutionId(),is(expected.getExecutionId()));

		if(expected.getEndTime() == null) {
			assertThat(actual.getEndTime(), nullValue());
		}
		else {
			assertThat(actual.getEndTime().getTime(), is(expected.getEndTime().getTime()));
		}
		if(expected.getStartTime() == null) {
			assertThat(actual.getStartTime(), nullValue());
		}
		else {
			assertThat(actual.getStartTime().getTime(), is(expected.getStartTime().getTime()));
		}
		assertThat(actual.getExitCode(), is(expected.getExitCode()));
		assertThat(actual.getExitMessage(), is(expected.getExitMessage()));
		assertThat(actual.getErrorMessage(), is(expected.getErrorMessage()));
		assertThat(actual.getParentExecutionId(),
				is(expected.getParentExecutionId()));
		assertThat(actual.getExternalExecutionId(),
				is(expected.getExternalExecutionId()));

		assertThat(actual.getArguments().size(),
				is(expected.getArguments().size()));
		expectedArgList.stream().forEach(
				expectedArg -> assertThat(actual.getArguments()
						.contains(expectedArg),is(true)));
	}

	@Test
	@DirtiesContext
	public void executeTaskWithNullIDReturnedTest() {
		boolean errorCaught = false;
		when(this.taskLauncher.launch(anyObject())).thenReturn(null);
		try {
			taskService.executeTask(TASK_NAME_ORIG, new HashMap<>(),
					new LinkedList<>());
		}
		catch(IllegalStateException ise) {
			errorCaught = true;
			assertEquals("Deployment ID is null for the task:myTask_ORIG", ise.getMessage());
		}
		if(!errorCaught) {
			fail();
		}
	}

	@Test
	@DirtiesContext
	public void executeTaskWithNullDefinitionTest() {
		boolean errorCaught = false;
		when(this.taskLauncher.launch(anyObject())).thenReturn("0");
		TaskService taskService =
				new DefaultTaskService(this.dataSourceProperties,
						mock(TaskDefinitionRepository.class), this.taskExplorer,
						this.taskExecutionRepository, this.appRegistry,
						this.resourceLoader, this.taskLauncher,
						this.metadataResolver, new TaskConfigurationProperties(),
						new InMemoryDeploymentIdRepository(), null);
		try {
			taskService.executeTask(TASK_NAME_ORIG, new HashMap<>(),
					new LinkedList<>());
		}
		catch(NoSuchTaskDefinitionException ise) {
			errorCaught = true;
			assertEquals("Could not find task definition named myTask_ORIG", ise.getMessage());
		}
		if(!errorCaught) {
			fail();
		}
	}

	@Test
	@DirtiesContext
	public void createSequenceComposedTask() {
		String dsl = "AAA && BBB";
		taskService.saveTaskDefinition("seqTask", dsl);
		verifyTaskExistsInRepo("seqTask", dsl);

		verifyTaskExistsInRepo("seqTask-AAA", "AAA");
		verifyTaskExistsInRepo("seqTask-BBB", "BBB");
	}

	@Test
	@DirtiesContext
	public void createSplitComposedTask() {
		String dsl = "<AAA || BBB>";
		taskService.saveTaskDefinition("splitTask", dsl);
		verifyTaskExistsInRepo("splitTask", dsl);

		verifyTaskExistsInRepo("splitTask-AAA", "AAA");
		verifyTaskExistsInRepo("splitTask-BBB", "BBB");
	}

	@Test
	@DirtiesContext
	public void verifyComposedTaskFlag() {
		String composedTaskDsl = "<AAA || BBB>";
		assertTrue("Expected true for composed task", taskService.isComposedDefinition(composedTaskDsl));
		composedTaskDsl = "AAA 'FAILED' -> BBB '*' -> CCC";
		assertTrue("Expected true for composed task", taskService.isComposedDefinition(composedTaskDsl));
		composedTaskDsl = "AAA && BBB && CCC";
		assertTrue("Expected true for composed task", taskService.isComposedDefinition(composedTaskDsl));
		String nonComposedTaskDsl = "AAA";
		assertFalse("Expected false for non-composed task" , taskService.isComposedDefinition(nonComposedTaskDsl));
		nonComposedTaskDsl = "AAA --foo=bar";
		assertFalse("Expected false for non-composed task" , taskService.isComposedDefinition(nonComposedTaskDsl));
	}

	@Test
	@DirtiesContext
	public void createTransitionComposedTask() {
		String dsl = "AAA 'FAILED' -> BBB '*' -> CCC";
		taskService.saveTaskDefinition("transitionTask", dsl);
		verifyTaskExistsInRepo("transitionTask", dsl);

		verifyTaskExistsInRepo("transitionTask-AAA", "AAA");
		verifyTaskExistsInRepo("transitionTask-BBB", "BBB");
	}

	@Test
	@DirtiesContext
	public void createSimpleTask() {
		taskService.saveTaskDefinition("simpleTask", "AAA --foo=bar");
		verifyTaskExistsInRepo("simpleTask", "AAA --foo=bar");
	}

	@Test
	@DirtiesContext
	public void deleteComposedTask() {
		String dsl = "AAA && BBB && CCC";
		taskService.saveTaskDefinition("deleteTask", dsl);
		verifyTaskExistsInRepo("deleteTask-AAA", "AAA");
		verifyTaskExistsInRepo("deleteTask-BBB", "BBB");
		verifyTaskExistsInRepo("deleteTask-CCC", "CCC");
		verifyTaskExistsInRepo("deleteTask", dsl);

		long preDeleteSize = taskDefinitionRepository.count();
		taskService.deleteTaskDefinition("deleteTask");
		assertThat(preDeleteSize - 4,
				is(equalTo(taskDefinitionRepository.count())));
	}

	@Test
	@DirtiesContext
	public void deleteComposedTaskDeleteOnlyChildren() {
		taskService.saveTaskDefinition("deleteTask-AAA", "AAA");
		String dsl = "BBB && CCC";
		taskService.saveTaskDefinition("deleteTask", dsl);
		verifyTaskExistsInRepo("deleteTask-AAA", "AAA");
		verifyTaskExistsInRepo("deleteTask-BBB", "BBB");
		verifyTaskExistsInRepo("deleteTask-CCC", "CCC");
		verifyTaskExistsInRepo("deleteTask", dsl);

		long preDeleteSize = taskDefinitionRepository.count();
		taskService.deleteTaskDefinition("deleteTask");
		assertThat(preDeleteSize - 3,
				is(equalTo(taskDefinitionRepository.count())));
		verifyTaskExistsInRepo("deleteTask-AAA", "AAA");
	}

	@Test
	@DirtiesContext
	public void deleteComposedTaskWithLabel() {
		String dsl = "LLL: AAA && BBB";
		taskService.saveTaskDefinition("deleteTask", dsl);
		verifyTaskExistsInRepo("deleteTask-LLL", "AAA");
		verifyTaskExistsInRepo("deleteTask-BBB", "BBB");
		verifyTaskExistsInRepo("deleteTask", dsl);

		long preDeleteSize = taskDefinitionRepository.count();
		taskService.deleteTaskDefinition("deleteTask");
		assertThat(preDeleteSize - 3,
				is(equalTo(taskDefinitionRepository.count())));
	}

	@Test
	@DirtiesContext
	public void verifyDataFlowUriProperty() throws Exception {
		when(this.taskLauncher.launch(anyObject())).thenReturn("0");
		TaskService taskService =
				new DefaultTaskService(this.dataSourceProperties,
						mock(TaskDefinitionRepository.class), this.taskExplorer,
						this.taskExecutionRepository, this.appRegistry,
						this.resourceLoader, this.taskLauncher,
						this.metadataResolver, new TaskConfigurationProperties(),
						new InMemoryDeploymentIdRepository(), "http://myserver:9191");
		List<String> cmdLineArgs = new ArrayList<>();
		Method method = ReflectionUtils.findMethod(DefaultTaskService.class, "updateDataFlowUriIfNeeded", Map.class, List.class);
		ReflectionUtils.makeAccessible(method);
		Map<String, String> appDeploymentProperties = new HashMap<>();
		method.invoke(taskService, appDeploymentProperties, cmdLineArgs);
		assertTrue(appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is expected to be in the app deployment properties", appDeploymentProperties.get("dataflowServerUri").equals("http://myserver:9191"));
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflow-server-uri", "http://localhost:8080");
		method.invoke(taskService, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is incorrect", appDeploymentProperties.get("dataflow-server-uri").equals("http://localhost:8080"));
		appDeploymentProperties.clear();
		appDeploymentProperties.put("dataflowServerUri", "http://localhost:8191");
		method.invoke(taskService, appDeploymentProperties, cmdLineArgs);
		assertTrue(appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is incorrect", appDeploymentProperties.get("dataflowServerUri").equals("http://localhost:8191"));
		appDeploymentProperties.clear();
		appDeploymentProperties.put("DATAFLOW_SERVER_URI", "http://localhost:9000");
		method.invoke(taskService, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue("dataflowServerUri is incorrect", appDeploymentProperties.get("DATAFLOW_SERVER_URI").equals("http://localhost:9000"));
		appDeploymentProperties.clear();
		cmdLineArgs.add("--dataflowServerUri=http://localhost:8383");
		method.invoke(taskService, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		cmdLineArgs.clear();
		cmdLineArgs.add("DATAFLOW_SERVER_URI=http://localhost:8383");
		method.invoke(taskService, appDeploymentProperties, cmdLineArgs);
		assertTrue(!appDeploymentProperties.containsKey("dataflowServerUri"));
		assertTrue(!appDeploymentProperties.containsKey("DATAFLOW-SERVER-URI"));
	}


	private void verifyTaskExistsInRepo(String taskName, String dsl) {
		TaskDefinition taskDefinition = taskDefinitionRepository.findOne(taskName);

		assertThat(taskDefinition.getName(), is(equalTo(taskName)));
		assertThat(taskDefinition.getDslText(),is(equalTo(dsl)));
	}
}
