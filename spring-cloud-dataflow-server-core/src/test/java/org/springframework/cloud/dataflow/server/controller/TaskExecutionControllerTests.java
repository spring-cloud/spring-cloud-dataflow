/*
 * Copyright 2016-2023 the original author or authors.
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

import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskDeployment;
import org.springframework.cloud.dataflow.core.TaskPlatform;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.config.DataflowAsyncAutoConfiguration;
import org.springframework.cloud.dataflow.server.config.apps.CommonApplicationProperties;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDeploymentRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.service.TaskDeleteService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionInfoService;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Glenn Renfro
 * @author Ilayaperumal Gopinathan
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Chris Bono
 * @author Corneil du Plessis
 */
@SpringBootTest(
		classes = { JobDependencies.class, TaskExecutionAutoConfiguration.class, DataflowAsyncAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class, BatchProperties.class})
@EnableConfigurationProperties({CommonApplicationProperties.class})
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
	private TaskExecutionDaoContainer daoContainer;

	@Autowired
	private JobRepositoryContainer jobRepositoryContainer;

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	private TaskBatchDaoContainer taskBatchDaoContainer;

	@Autowired
	private AppRegistryService appRegistryService;

	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private AggregateTaskExplorer taskExplorer;

	@Autowired
	private AggregateExecutionSupport aggregateExecutionSupport;

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

	@Autowired
	private TaskJobService taskJobService;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;


	@BeforeEach
	public void setupMockMVC() {
		assertThat(this.launcherRepository.findByName("default")).isNull();
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
			SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(TASK_NAME_ORIG, taskDefinitionReader);
			TaskExecutionDao dao = daoContainer.get(schemaVersionTarget.getName());
			TaskExecution taskExecution1 =
					dao.createTaskExecution(TASK_NAME_ORIG, new Date(), SAMPLE_ARGUMENT_LIST, "foobar");

			dao.createTaskExecution(TASK_NAME_ORIG, new Date(), SAMPLE_ARGUMENT_LIST, "foobar", taskExecution1.getExecutionId());
			dao.createTaskExecution(TASK_NAME_FOO, new Date(), SAMPLE_ARGUMENT_LIST, null);
			TaskExecution taskExecution = dao.createTaskExecution(TASK_NAME_FOOBAR, new Date(), SAMPLE_ARGUMENT_LIST,
					null);
			SchemaVersionTarget fooBarTarget = aggregateExecutionSupport.findSchemaVersionTarget(TASK_NAME_FOOBAR, taskDefinitionReader);
			JobRepository jobRepository = jobRepositoryContainer.get(fooBarTarget.getName());
			JobInstance instance = jobRepository.createJobInstance(TASK_NAME_FOOBAR, new JobParameters());
			JobExecution jobExecution = jobRepository.createJobExecution(instance, new JobParameters(), null);
			TaskBatchDao taskBatchDao = taskBatchDaoContainer.get(fooBarTarget.getName());
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

	@Test
	void taskExecutionControllerConstructorMissingExplorer() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskExecutionController(
				null,
				aggregateExecutionSupport,
				taskExecutionService,
				taskDefinitionRepository,
				taskDefinitionReader, taskExecutionInfoService,
				taskDeleteService,
				taskJobService));
	}

	@Test
	void taskExecutionControllerConstructorMissingTaskService() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskExecutionController(
				taskExplorer,
				aggregateExecutionSupport,
				null,
				taskDefinitionRepository,
				taskDefinitionReader,
				taskExecutionInfoService,
				taskDeleteService,
				taskJobService));
	}

	@Test
	void taskExecutionControllerConstructorMissingTaskDefinitionRepository() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskExecutionController(
				taskExplorer,
				aggregateExecutionSupport,
				taskExecutionService,
				null,
				taskDefinitionReader, taskExecutionInfoService,
				taskDeleteService,
				taskJobService));
	}

	@Test
	void taskExecutionControllerConstructorMissingTaskDefinitionRetriever() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskExecutionController(taskExplorer,
				aggregateExecutionSupport,
				taskExecutionService,
				taskDefinitionRepository,
				taskDefinitionReader, null,
				taskDeleteService,
				taskJobService));
	}

	@Test
	void taskExecutionControllerConstructorMissingDeleteTaskService() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskExecutionController(taskExplorer,
				aggregateExecutionSupport,
				taskExecutionService,
				taskDefinitionRepository,
				taskDefinitionReader, taskExecutionInfoService,
				null,
				taskJobService));
	}

	@Test
	void taskExecutionControllerConstructorMissingDeleteTaskJobService() {
		assertThatIllegalArgumentException().isThrownBy(() -> new TaskExecutionController(taskExplorer,
				aggregateExecutionSupport,
				taskExecutionService,
				taskDefinitionRepository,
				taskDefinitionReader, taskExecutionInfoService,
				taskDeleteService,
				null));
	}

	@Test
	void getExecutionNotFound() throws Exception {
		mockMvc.perform(get("/tasks/executions/1345345345345").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isNotFound());
	}

	@Test
	void getExecution() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "",
				mockMvc.perform(get("/tasks/executions/1").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(content().json("{taskName: \"" + TASK_NAME_ORIG + "\"}"))
				.andExpect(jsonPath("$.parentExecutionId", is(nullValue())))
				.andExpect(jsonPath("jobExecutionIds", hasSize(0)));
	}

	@Test
	void getChildTaskExecution() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "",
				mockMvc.perform(get("/tasks/executions/2").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$.parentExecutionId", is(1)))
				.andExpect(jsonPath("jobExecutionIds", hasSize(0)));
	}

	@Test
	void getExecutionForJob() throws Exception {
		mockMvc.perform(get("/tasks/executions/4").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().json("{taskName: \"" + TASK_NAME_FOOBAR + "\"}"))
				.andExpect(jsonPath("jobExecutionIds[0]", is(1)))
				.andExpect(jsonPath("jobExecutionIds", hasSize(1)));
	}

	@Test
	void getAllExecutions() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$._embedded.taskExecutionResourceList[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[*].parentExecutionId", containsInAnyOrder(null, null, null, 1)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(4)));
	}

	@Test
	void getAllThinExecutions() throws Exception {
			mockMvc.perform(get("/tasks/thinexecutions").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
			.andExpect(jsonPath("$._embedded.taskExecutionThinResourceList[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
			.andExpect(jsonPath("$._embedded.taskExecutionThinResourceList[*].parentExecutionId", containsInAnyOrder(null, null, null, 1)))
			.andExpect(jsonPath("$._embedded.taskExecutionThinResourceList", hasSize(4)));
	}

	@Test
	void getCurrentExecutions() throws Exception {
		when(taskLauncher.getRunningTaskExecutionCount()).thenReturn(4);
		mockMvc.perform(get("/tasks/executions/current").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].runningExecutionCount", is(4)));

	}

	@Test
	void boot3Execution() throws Exception {
		if (appRegistryService.getDefaultApp("timestamp3", ApplicationType.task) == null) {
			appRegistryService.save("timestamp3",
					ApplicationType.task,
					"3.0.0",
					new URI("file:src/test/resources/apps/foo-task"),
					null,
					AppBootSchemaVersion.BOOT3);
		}
		taskDefinitionRepository.save(new TaskDefinition("timestamp3", "timestamp3"));
		when(taskLauncher.launch(any())).thenReturn("abc");

		ResultActions resultActions = mockMvc.perform(
						post("/tasks/executions/launch")
								.queryParam("name", "timestamp3")
								.queryParam("properties", "app.timestamp3.foo3=bar3,app.timestamp3.bar3=3foo")
								.accept(MediaType.APPLICATION_JSON)
				).andDo(print())
				.andExpect(status().isCreated());

		String response = resultActions.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new Jackson2HalModule());
		mapper.registerModule(new Jackson2DataflowModule());
		LaunchResponseResource resource = mapper.readValue(response, LaunchResponseResource.class);
		resultActions = mockMvc.perform(
						get("/tasks/executions/" + resource.getExecutionId())
								.accept(MediaType.APPLICATION_JSON)
								.queryParam("schemaTarget", resource.getSchemaTarget())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().json("{taskName: \"timestamp3\"}"));
		response = resultActions.andReturn().getResponse().getContentAsString();
		System.out.println("response=" + response);
		JsonNode json;
		try (JsonParser parser = new ObjectMapper().createParser(response)) {
			json = parser.readValueAs(JsonNode.class);
		}
		System.out.println("json=" + json.toPrettyString());
		assertThat(json.findValue("deploymentProperties")).isNotNull();
		JsonNode deploymentProperties = json.findValue("deploymentProperties");
		System.out.println("deploymentProperties=" + deploymentProperties.toPrettyString());
		assertThat(deploymentProperties.hasNonNull("app.timestamp3.spring.cloud.task.tablePrefix")).isTrue();
		assertThat(deploymentProperties.get("app.timestamp3.spring.cloud.task.tablePrefix").asText()).isEqualTo("BOOT3_TASK_");
	}

	@Test
	void invalidBoot3Execution() throws Exception {
		if (appRegistryService.getDefaultApp("timestamp3", ApplicationType.task) == null) {
			appRegistryService.save("timestamp3",
					ApplicationType.task,
					"3.0.0",
					new URI("file:src/test/resources/apps/foo-task"),
					null,
					AppBootSchemaVersion.BOOT3);
		}
		taskDefinitionRepository.save(new TaskDefinition("timestamp3", "timestamp3"));
		when(taskLauncher.launch(any())).thenReturn("abc");

		ResultActions resultActions = mockMvc.perform(
						post("/tasks/executions")
								.queryParam("name", "timestamp3")
								.accept(MediaType.APPLICATION_JSON)
				).andDo(print())
				.andExpect(status().isBadRequest());

		String response = resultActions.andReturn().getResponse().getContentAsString();
		assertThat(response).contains("cannot be launched for");
	}

	@Test
	void boot2Execution() throws Exception {
		if (appRegistryService.getDefaultApp("timestamp2", ApplicationType.task) == null) {
			appRegistryService.save("timestamp2",
					ApplicationType.task,
					"2.0.1",
					new URI("file:src/test/resources/apps/foo-task"),
					null,
					AppBootSchemaVersion.BOOT2);
		}
		taskDefinitionRepository.save(new TaskDefinition("timestamp2", "timestamp2"));
		when(taskLauncher.launch(any())).thenReturn("abc");

		ResultActions resultActions = mockMvc.perform(
						post("/tasks/executions/launch")
								.queryParam("name", "timestamp2")
								.queryParam("properties", "app.timestamp2.foo3=bar3,app.timestamp2.bar3=3foo")
								.accept(MediaType.APPLICATION_JSON)
				).andDo(print())
				.andExpect(status().isCreated());

		String response = resultActions.andReturn().getResponse().getContentAsString();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new Jackson2HalModule());
		mapper.registerModule(new Jackson2DataflowModule());
		LaunchResponseResource resource = mapper.readValue(response, LaunchResponseResource.class);
		resultActions = mockMvc.perform(
						get("/tasks/executions/" + resource.getExecutionId())
								.accept(MediaType.APPLICATION_JSON)
								.queryParam("schemaTarget", resource.getSchemaTarget())
				)
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().json("{taskName: \"timestamp2\"}"));
		response = resultActions.andReturn().getResponse().getContentAsString();
		System.out.println("response=" + response);
		JsonNode json;
		try (JsonParser parser = new ObjectMapper().createParser(response)) {
			json = parser.readValueAs(JsonNode.class);
		}
		System.out.println("json=" + json.toPrettyString());
		assertThat(json.findValue("deploymentProperties")).isNotNull();
		JsonNode deploymentProperties = json.findValue("deploymentProperties");
		System.out.println("deploymentProperties=" + deploymentProperties.toPrettyString());
		assertThat(deploymentProperties.hasNonNull("app.timestamp2.spring.cloud.task.tablePrefix")).isTrue();
		assertThat(deploymentProperties.get("app.timestamp2.spring.cloud.task.tablePrefix").asText()).isEqualTo("TASK_");

	}

	@Test
	void getExecutionsByName() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$._embedded.taskExecutionResourceList[0].",
				mockMvc.perform(get("/tasks/executions/").param("name", TASK_NAME_ORIG).accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[0].taskName", is(TASK_NAME_ORIG)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[1].taskName", is(TASK_NAME_ORIG)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[0].jobExecutionIds", hasSize(0)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[1].jobExecutionIds", hasSize(0)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(2)));
	}

	@Test
	void getExecutionsByNameNotFound() throws Exception {
		mockMvc.perform(get("/tasks/executions/").param("name", "BAZ").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString()
				.contains("NoSuchTaskException");
	}

	@Test
	void cleanup() throws Exception {
		mockMvc.perform(delete("/tasks/executions/1"))
				.andDo(print())
				.andExpect(status().is(200));
		verify(taskLauncher).cleanup("foobar");
	}

	@Test
	void cleanupAll() throws Exception {
		mockMvc.perform(delete("/tasks/executions"))
				.andDo(print())
				.andExpect(status().is(200));
		verify(taskLauncher,  times(2)).cleanup("foobar");
	}

	@Test
	void cleanupWithActionParam() throws Exception {
		mockMvc.perform(delete("/tasks/executions/1").param("action", "CLEANUP"))
				.andDo(print())
				.andExpect(status().is(200));
		verify(taskLauncher).cleanup("foobar");
	}

	@Test
	void cleanupWithInvalidAction() throws Exception {
		mockMvc.perform(delete("/tasks/executions/1").param("action", "does_not_exist").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().is(400))
				.andExpect(jsonPath("_embedded.errors[0].message",
						is("The parameter 'action' must contain one of the following values: 'CLEANUP, REMOVE_DATA'.")));
	}

	@Test
	void cleanupByIdNotFound() throws Exception {
		mockMvc.perform(delete("/tasks/executions/10"))
				.andDo(print())
				.andExpect(status().is(404))
				.andReturn()
				.getResponse()
				.getContentAsString().contains("NoSuchTaskExecutionException");
	}

	@Test
	void deleteSingleTaskExecutionById() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$._embedded.taskExecutionResourceList[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(4)));
		mockMvc.perform(delete("/tasks/executions/1").param("action", "REMOVE_DATA"))
				.andDo(print())
				.andExpect(status().isOk());
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$._embedded.taskExecutionResourceList[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[*].executionId", containsInAnyOrder(4, 3)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(2)));
	}

	/**
	 * This test will successfully delete 3 task executions. 2 task executions are specified in the arguments.
	 * But since the task execution with id `1` is a parent task execution with 1 child, that child task
	 * execution will be deleted as well.
	 */
	@Test
	void deleteThreeTaskExecutionsById() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$._embedded.taskExecutionResourceList[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(4)));
		mockMvc.perform(delete("/tasks/executions/1,3").param("action", "REMOVE_DATA"))
				.andDo(print())
				.andExpect(status().isOk());
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$._embedded.taskExecutionResourceList[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[*].executionId", containsInAnyOrder(4)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(1)));
	}

	@Test
	void deleteAllTaskExecutions() throws Exception {
		verifyTaskArgs(SAMPLE_CLEANSED_ARGUMENT_LIST, "$._embedded.taskExecutionResourceList[0].",
				mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON))
						.andDo(print())
						.andExpect(status().isOk()))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList[*].executionId", containsInAnyOrder(4, 3, 2, 1)))
				.andExpect(jsonPath("$._embedded.taskExecutionResourceList", hasSize(4)));
		mockMvc.perform(delete("/tasks/executions").param("action", "CLEANUP,REMOVE_DATA"))
				.andDo(print())
				.andExpect(status().isOk());
		mockMvc.perform(get("/tasks/executions/").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page.totalElements", is(0)));
	}

	private ResultActions verifyTaskArgs(List<String> expectedArgs, String prefix, ResultActions ra) throws Exception {
		ra.andExpect(jsonPath(prefix + "arguments", hasSize(expectedArgs.size())));
		for (int argCount = 0; argCount < expectedArgs.size(); argCount++) {
			ra.andExpect(jsonPath(String.format(prefix + "arguments[%d]", argCount), is(expectedArgs.get(argCount))));
		}
		return ra;
	}

	@Test
	void sorting() throws Exception {
		mockMvc.perform(get("/tasks/executions").param("sort", "TASK_EXECUTION_ID").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk());
		mockMvc.perform(get("/tasks/executions").param("sort", "task_execution_id").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().isOk());

		mockMvc.perform(get("/tasks/executions").param("sort", "SCHEMA_TARGET").accept(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());
		mockMvc.perform(get("/tasks/executions").param("sort", "schema_target").accept(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());

		mockMvc.perform(get("/tasks/executions").param("sort", "WRONG_FIELD").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().is5xxServerError())
				.andExpect(content().string(containsString("Sorting column WRONG_FIELD not allowed")));
		mockMvc.perform(get("/tasks/executions").param("sort", "wrong_field").accept(MediaType.APPLICATION_JSON))
				.andDo(print())
				.andExpect(status().is5xxServerError())
				.andExpect(content().string(containsString("Sorting column wrong_field not allowed")));
	}

}
