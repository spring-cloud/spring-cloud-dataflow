/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Gunnar Hillert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { JobDependencies.class, PropertyPlaceholderAutoConfiguration.class, BatchProperties.class })
@EnableConfigurationProperties({ CommonApplicationProperties.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class TaskExecutionControllerTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	private final static String TASK_NAME_FOO = BASE_TASK_NAME + "_FOO";

	private final static String TASK_NAME_FOOBAR = BASE_TASK_NAME + "_FOOBAR";

	private boolean initialized = false;

	private static List<String> SAMPLE_ARGUMENT_LIST;

	private static List<String> SAMPLE_CLEANSED_ARGUMENT_LIST;

	@Autowired
	private TaskExecutionDao dao;

	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	private TaskBatchDao taskBatchDao;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private TaskExplorer taskExplorer;

	@Autowired
	private TaskExecutionService taskExecutionService;

	@Autowired
	private TaskLauncher taskLauncher;

	@Autowired
	private LauncherRepository launcherRepository;

	@Autowired
	private TaskPlatform taskPlatform;

	@Autowired
	private TaskExecutionInfoService taskExecutionInfoService;

	@Autowired
	private TaskDeleteService taskDeleteService;

	@Autowired
	private TaskDeploymentRepository taskDeploymentRepository;

	@Before
	public void setupMockMVC() {
		Launcher launcher = new Launcher("default", "local", taskLauncher);
		launcherRepository.save(launcher);
		taskPlatform.setLaunchers(Collections.singletonList(launcher));
		this.mockMvc = MockMvcBuilders.webAppContextSetup(wac)
				.defaultRequest(get("/").accept(MediaType.APPLICATION_JSON)).build();
		if (!initialized) {
			SAMPLE_ARGUMENT_LIST = new LinkedList<String>();
			SAMPLE_ARGUMENT_LIST.add("--password=foo");
			SAMPLE_ARGUMENT_LIST.add("password=bar");
			SAMPLE_ARGUMENT_LIST.add("org.woot.password=baz");
			SAMPLE_ARGUMENT_LIST.add("foo.bar=foo");
			SAMPLE_ARGUMENT_LIST.add("bar.baz = boo");
			SAMPLE_ARGUMENT_LIST.add("foo.credentials.boo=bar");
			SAMPLE_ARGUMENT_LIST.add("spring.datasource.username=dbuser");
			SAMPLE_ARGUMENT_LIST.add("spring.datasource.password=dbpass");

			SAMPLE_CLEANSED_ARGUMENT_LIST = new LinkedList<String>();
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("--password=******");
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("password=******");
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("org.woot.password=******");
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("foo.bar=foo");
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("bar.baz = boo");
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("foo.credentials.boo=******");
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("spring.datasource.username=******");
			SAMPLE_CLEANSED_ARGUMENT_LIST.add("spring.datasource.password=******");

			taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
			TaskExecution taskExecution1 =
				dao.createTaskExecution(TASK_NAME_ORIG, new Date(), SAMPLE_ARGUMENT_LIST, "foobar");

			dao.createTaskExecution(TASK_NAME_ORIG, new Date(), SAMPLE_ARGUMENT_LIST, "foobar", taskExecution1.getExecutionId());
			dao.createTaskExecution(TASK_NAME_FOO, new Date(), SAMPLE_ARGUMENT_LIST, null);
			TaskExecution taskExecution = dao.createTaskExecution(TASK_NAME_FOOBAR, new Date(), SAMPLE_ARGUMENT_LIST,
					null);
			JobInstance instance = jobRepository.createJobInstance(TASK_NAME_FOOBAR, new JobParameters());
			JobExecution jobExecution = jobRepository.createJobExecution(instance, new JobParameters(), null);
			taskBatchDao.saveRelationship(taskExecution, jobExecution);
			TaskDeployment taskDeployment = new TaskDeployment();
			taskDeployment.setTaskDefinitionName(TASK_NAME_ORIG);
			taskDeployment.setTaskDeploymentId("foobar");
			taskDeployment.setPlatformName("default");
			taskDeployment.setCreatedOn(Instant.now());
			taskDeploymentRepository.save(taskDeployment);
			initialized = true;
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingExplorer() {
		new TaskExecutionController(null, taskExecutionService, taskDefinitionRepository, taskExecutionInfoService,
				taskDeleteService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingTaskService() {
		new TaskExecutionController(taskExplorer, null, taskDefinitionRepository, taskExecutionInfoService,
				taskDeleteService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingTaskDefinitionRepository() {
		new TaskExecutionController(taskExplorer, taskExecutionService, null, taskExecutionInfoService,
				taskDeleteService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingTaskDefinitionRetriever() {
		new TaskExecutionController(taskExplorer, taskExecutionService, taskDefinitionRepository, null,
				taskDeleteService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTaskExecutionControllerConstructorMissingDeleteTaskService() {
		new TaskExecutionController(taskExplorer, taskExecutionService, taskDefinitionRepository,
				taskExecutionInfoService, null);
	}

	@Test
	public void testGetExecutionNotFound() throws Exception {
		mockMvc.perform(get("/tasks/executions/1345345345345").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isNotFound());
	}

	@Test
	public void testGetExecution() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "",
				mockMvc.perform(get("/tasks/executions/1").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk()).andExpect(content().json("{taskName: \"" + TASK_NAME_ORIG + "\"}"))
						.andExpect(jsonPath("$.parentExecutionId", is(nullValue())))
						.andExpect(jsonPath("jobExecutionIds", hasSize(0))));
	}

	@Test
	public void testGetChildTaskExecution() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "",
				mockMvc.perform(get("/tasks/executions/2").accept(MediaType.APPLICATION_JSON))
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.parentExecutionId", is(1)))
						.andExpect(jsonPath("jobExecutionIds", hasSize(0))));
	}

	@Test
	public void testGetExecutionForJob() throws Exception {
		mockMvc.perform(get("/tasks/executions/4").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(content().json("{taskName: \"" + TASK_NAME_FOOBAR + "\"}"))
				.andExpect(jsonPath("jobExecutionIds[0]", is(1))).andExpect(jsonPath("jobExecutionIds", hasSize(1)));
	}

	@Test
	public void testGetAllExecutions() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$.content[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
						.andExpect(jsonPath("$.content[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
						.andExpect(jsonPath("$.content[*].parentExecutionId", containsInAnyOrder(null, null, null, 1)))
						.andExpect(jsonPath("$.content", hasSize(4))));
	}

	@Test
	public void testGetCurrentExecutions() throws Exception {
		when(taskLauncher.getRunningTaskExecutionCount()).thenReturn(4);
		mockMvc.perform(get("/tasks/executions/current").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].runningExecutionCount", is(4)));

	}

	@Test
	public void testGetExecutionsByName() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$.content[0].", mockMvc
				.perform(get("/tasks/executions/").param("name", TASK_NAME_ORIG).accept(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].taskName", is(TASK_NAME_ORIG)))
				.andExpect(jsonPath("$.content[1].taskName", is(TASK_NAME_ORIG)))
				.andExpect(jsonPath("$.content[0].jobExecutionIds", hasSize(0)))
				.andExpect(jsonPath("$.content[1].jobExecutionIds", hasSize(0)))
				.andExpect(jsonPath("$.content", hasSize(2))));
	}

	@Test
	public void testGetExecutionsByNameNotFound() throws Exception {
		mockMvc.perform(get("/tasks/executions/").param("name", "BAZ").accept(MediaType.APPLICATION_JSON))
				.andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString()
				.contains("NoSuchTaskException");
	}

	@Test
	public void testCleanup() throws Exception {
		mockMvc.perform(delete("/tasks/executions/1")).andExpect(status().is(200));
		verify(taskLauncher).cleanup("foobar");
	}

	@Test
	public void testCleanupWithActionParam() throws Exception {
		mockMvc.perform(delete("/tasks/executions/1").param("action", "CLEANUP")).andExpect(status().is(200));
		verify(taskLauncher).cleanup("foobar");
	}

	@Test
	public void testCleanupWithInvalidAction() throws Exception {
		mockMvc.perform(delete("/tasks/executions/1").param("action", "does_not_exist").accept(MediaType.APPLICATION_JSON))
		.andDo(print())
		.andExpect(status().is(400))
		.andExpect(jsonPath("content[0].message", is("The parameter 'action' must contain one of the following values: 'CLEANUP, REMOVE_DATA'.")));
	}

	@Test
	public void testCleanupByIdNotFound() throws Exception {
		mockMvc.perform(delete("/tasks/executions/10")).andExpect(status().is(404)).andReturn().getResponse()
				.getContentAsString().contains("NoSuchTaskExecutionException");
	}

	@Test
	public void testDeleteSingleTaskExecutionById() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$.content[0].",
			mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
					.andExpect(jsonPath("$.content[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
					.andExpect(jsonPath("$.content", hasSize(4))));
		mockMvc.perform(delete("/tasks/executions/1").param("action", "REMOVE_DATA"))
			.andExpect(status().isOk());
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$.content[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
						.andExpect(jsonPath("$.content[*].executionId", containsInAnyOrder(4, 3)))
						.andExpect(jsonPath("$.content", hasSize(2))));
	}

	/**
	 * This test will successfully delete 3 task executions. 2 task executions are specified in the arguments.
	 * But since the task execution with id `1` is a parent task execution with 1 child, that child task
	 * execution will be deleted as well.
	 *
	 */
	@Test
	public void testDeleteThreeTaskExecutionsById() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$.content[0].",
			mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
					.andExpect(jsonPath("$.content[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
					.andExpect(jsonPath("$.content", hasSize(4))));
		mockMvc.perform(delete("/tasks/executions/1,3").param("action", "REMOVE_DATA"))
			.andExpect(status().isOk());
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$.content[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
						.andExpect(jsonPath("$.content[*].executionId", containsInAnyOrder(4)))
						.andExpect(jsonPath("$.content", hasSize(1))));
	}

	@Test
	public void testDeleteNonParentTaskExecutionById() throws Exception {
		mockMvc.perform(delete("/tasks/executions/2"))
			.andDo(print())
			.andExpect(status().is(400))
			.andExpect(jsonPath("content[0].logref", is("CannotDeleteNonParentTaskExecutionException")))
			.andExpect(jsonPath("content[0].message", is("Cannot delete non-parent TaskExecution with id 1")));
	}

	private ResultActions verifyTaskArgs(List<String> expectedArgs, String prefix, ResultActions ra) throws Exception {
		ra.andExpect(jsonPath(prefix + "arguments", hasSize(expectedArgs.size())));
		for (int argCount = 0; argCount < expectedArgs.size(); argCount++) {
			ra.andExpect(jsonPath(String.format(prefix + "arguments[%d]", argCount), is(expectedArgs.get(argCount))));
		}
		return ra;
	}

	@Test
	public void testSorting() throws Exception {
		mockMvc.perform(get("/tasks/executions").param("sort", "TASK_EXECUTION_ID").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		mockMvc.perform(get("/tasks/executions").param("sort", "task_execution_id").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());

		mockMvc.perform(get("/tasks/executions").param("sort", "WRONG_FIELD").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is5xxServerError())
			.andExpect(content().string(containsString("Sorting column WRONG_FIELD not allowed")));
		mockMvc.perform(get("/tasks/executions").param("sort", "wrong_field").accept(MediaType.APPLICATION_JSON))
			.andExpect(status().is5xxServerError())
			.andExpect(content().string(containsString("Sorting column wrong_field not allowed")));
	}
}
