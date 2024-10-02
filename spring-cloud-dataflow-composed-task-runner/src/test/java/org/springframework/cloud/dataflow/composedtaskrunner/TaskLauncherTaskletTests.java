/*
 * Copyright 2017-2024 the original author or authors.
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

package org.springframework.cloud.dataflow.composedtaskrunner;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.ComposedTaskException;
import org.springframework.cloud.dataflow.composedtaskrunner.support.TaskExecutionTimeoutException;
import org.springframework.cloud.dataflow.composedtaskrunner.support.UnexpectedTaskExecutionException;
import org.springframework.cloud.dataflow.rest.client.DataFlowClientException;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.support.jackson.Jackson2DataflowModule;
import org.springframework.cloud.task.batch.listener.support.JdbcTaskBatchDao;
import org.springframework.cloud.task.configuration.TaskProperties;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.cloud.task.repository.TaskRepository;
import org.springframework.cloud.task.repository.dao.JdbcTaskExecutionDao;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.cloud.task.repository.support.SimpleTaskExplorer;
import org.springframework.cloud.task.repository.support.SimpleTaskRepository;
import org.springframework.cloud.task.repository.support.TaskExecutionDaoFactoryBean;
import org.springframework.cloud.task.repository.support.TaskRepositoryInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;


/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@SpringJUnitConfig(classes = {EmbeddedDataSourceConfiguration.class,
		org.springframework.cloud.dataflow.composedtaskrunner.TaskLauncherTaskletTests.TestConfiguration.class})
class TaskLauncherTaskletTests {
	private final static Logger logger = LoggerFactory.getLogger(TaskLauncherTaskletTests.class);
	private static final String TASK_NAME = "testTask1_0";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	@Autowired
	private TaskRepositoryInitializer taskRepositoryInitializer;

	@Autowired
	private JdbcTaskExecutionDao taskExecutionDao;

	@Autowired
	private Environment environment;
	private TaskOperations taskOperations;

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;

	private ObjectMapper mapper;


	@BeforeEach
	void setup() throws Exception{
		if (this.mapper == null) {
			this.mapper = new ObjectMapper();
			this.mapper.registerModule(new Jdk8Module());
			this.mapper.registerModule(new Jackson2HalModule());
			this.mapper.registerModule(new JavaTimeModule());
			this.mapper.registerModule(new Jackson2DataflowModule());
		}
		this.taskRepositoryInitializer.setDataSource(this.dataSource);
		this.taskRepositoryInitializer.afterPropertiesSet();
		this.taskOperations = mock(TaskOperations.class);
		TaskExecutionDaoFactoryBean taskExecutionDaoFactoryBean =
			new TaskExecutionDaoFactoryBean(this.dataSource);
		this.taskRepository = new SimpleTaskRepository(taskExecutionDaoFactoryBean);
		this.taskExplorer = new SimpleTaskExplorer(taskExecutionDaoFactoryBean);
		this.composedTaskProperties.setIntervalTimeBetweenChecks(500);
	}

	@Test
	@DirtiesContext
	void taskLauncherTasklet() {
		createCompleteTaskExecution(0);
		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);

		mockReturnValForTaskExecution(2L);
		chunkContext = chunkContext();
		createCompleteTaskExecution(0);
		taskLauncherTasklet = getTaskExecutionTasklet();
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(2L);
	}

	@Test
	@DirtiesContext
	void invalidTaskOperations() {
		TaskLauncherTasklet taskLauncherTasklet = new TestTaskLauncherTasklet(
				null,
				null,
				this.taskExplorer,
				this.composedTaskProperties,
				TASK_NAME,
				new TaskProperties(),
				environment,
				mapper
		);
		assertThatThrownBy(
				() -> execute(taskLauncherTasklet, null, chunkContext())
		).isInstanceOf(ComposedTaskException.class)
			.hasMessage(
				"Unable to connect to Data Flow Server to execute task operations. " +
						"Verify that Data Flow Server's tasks/definitions endpoint can be accessed.");
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletWithTaskExecutionId() {
		TaskProperties taskProperties = new TaskProperties();
		taskProperties.setExecutionid(88L);
		mockReturnValForTaskExecution(2L);
		ChunkContext chunkContext = chunkContext();
		createCompleteTaskExecution(0);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet(taskProperties);
		taskLauncherTasklet.setArguments(null);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(2L);
		assertThat(((List<?>) chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-arguments")).get(0)).isEqualTo("--spring.cloud.task.parent-execution-id=88");
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletWithoutTaskExecutionId() {

		mockReturnValForTaskExecution(2L);
		ChunkContext chunkContext = chunkContext();
		JobExecution jobExecution = new JobExecution(0L, new JobParameters());

		createAndStartCompleteTaskExecution(0, jobExecution);

		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		taskLauncherTasklet.setArguments(null);
		StepExecution stepExecution = new StepExecution("stepName", jobExecution, 0L);
		StepContribution contribution = new StepContribution(stepExecution);
		execute(taskLauncherTasklet, contribution, chunkContext);
		ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
		logger.info("execution-context:{}", executionContext.entrySet());
		assertThat(executionContext.get("task-execution-id")).isEqualTo(2L);
		assertThat(executionContext.get("task-arguments")).as("task-arguments not null").isNotNull();
		assertThat(((List<?>) executionContext.get("task-arguments")).get(0)).isEqualTo("--spring.cloud.task.parent-execution-id=1");
	}

	@SuppressWarnings("unchecked")
	@Test
	@DirtiesContext
	void taskLauncherTaskletWithTaskExecutionIdWithPreviousParentID() {

		TaskProperties taskProperties = new TaskProperties();
		taskProperties.setExecutionid(88L);
		mockReturnValForTaskExecution(2L);
		ChunkContext chunkContext = chunkContext();
		createCompleteTaskExecution(0);
		ExecutionContext executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
		executionContext.put("task-arguments", new ArrayList<String>());
		List<String> taskArguments = (List<String>) executionContext.get("task-arguments");
		assertThat(taskArguments).isNotNull().as("taskArguments");
		taskArguments.add("--spring.cloud.task.parent-execution-id=84");
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet(taskProperties);
		taskLauncherTasklet.setArguments(null);
		execute(taskLauncherTasklet, null, chunkContext);
		executionContext = chunkContext.getStepContext().getStepExecution().getExecutionContext();
		taskArguments = (List<String>) executionContext.get("task-arguments");
		assertThat(executionContext.get("task-execution-id")).isEqualTo(2L);
		assertThat(((List<?>) taskArguments).get(0)).isEqualTo("--spring.cloud.task.parent-execution-id=88");
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletStartTimeout() {
		mockReturnValForTaskExecution(1L);
		this.composedTaskProperties.setMaxStartWaitTime(500);
		this.composedTaskProperties.setIntervalTimeBetweenChecks(1000);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		assertThatThrownBy(() -> execute(taskLauncherTasklet, null, chunkContext))
			.isInstanceOf(TaskExecutionTimeoutException.class)
			.hasMessage("Timeout occurred during startup of task with Execution Id 1");

		createCompleteTaskExecution(0);
		this.composedTaskProperties.setMaxStartWaitTime(500);
		this.composedTaskProperties.setIntervalTimeBetweenChecks(1000);
		TaskLauncherTasklet taskLauncherTaskletNoTimeout = getTaskExecutionTasklet();
		execute(taskLauncherTaskletNoTimeout, null, chunkContext);
		// expect no exception
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletTimeout() {
		mockReturnValForTaskExecution(1L);
		this.composedTaskProperties.setMaxWaitTime(500);
		this.composedTaskProperties.setIntervalTimeBetweenChecks(1000);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		assertThatThrownBy(() -> execute(taskLauncherTasklet, null, chunkContext))
			.isInstanceOf(TaskExecutionTimeoutException.class)
			.hasMessage("Timeout occurred while processing task with Execution Id 1");
	}

	@Test
	@DirtiesContext
	void invalidTaskName() {
		final String ERROR_MESSAGE =
				"Could not find task definition named " + TASK_NAME;
		VndErrors errors = new VndErrors("message", ERROR_MESSAGE, Link.of("ref"));
		Mockito.doThrow(new DataFlowClientException(errors))
				.when(this.taskOperations)
				.launch(ArgumentMatchers.anyString(),
						ArgumentMatchers.any(),
						ArgumentMatchers.any());
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		assertThatThrownBy(
				() -> taskLauncherTasklet.execute(null, chunkContext)
		).isInstanceOf(DataFlowClientException.class).hasMessage(ERROR_MESSAGE);
	}

	@Test
	@DirtiesContext
	void noDataFlowServer() {
		final String ERROR_MESSAGE =
				"I/O error on GET request for \"http://localhost:9393\": Connection refused; nested exception is java.net.ConnectException: Connection refused";
		Mockito.doThrow(new ResourceAccessException(ERROR_MESSAGE))
				.when(this.taskOperations).launch(ArgumentMatchers.anyString(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any());
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		assertThatThrownBy(() -> execute(taskLauncherTasklet, null, chunkContext))
			.isInstanceOf(ResourceAccessException.class)
			.hasMessage(ERROR_MESSAGE);
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletFailure() {
		mockReturnValForTaskExecution(1L);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		createCompleteTaskExecution(1, "This is the exit message of the task itself.");
		assertThatThrownBy(() -> execute(taskLauncherTasklet, null, chunkContext))
			.isInstanceOf(UnexpectedTaskExecutionException.class)
			.hasMessage("Task returned a non zero exit code.")
			.matches(x -> ((UnexpectedTaskExecutionException) x).getExitCode() == 1)
			.matches(x -> ((UnexpectedTaskExecutionException) x).getExitMessage()
				.equals("This is the exit message of the task itself."))
			.matches(x -> ((UnexpectedTaskExecutionException) x).getEndTime() != null);
	}

	private RepeatStatus execute(TaskLauncherTasklet taskLauncherTasklet, StepContribution contribution,
			ChunkContext chunkContext) {
		RepeatStatus status = taskLauncherTasklet.execute(contribution, chunkContext);
		if (!status.isContinuable()) {
			throw new IllegalStateException("Expected continuable status for the first execution.");
		}
		return taskLauncherTasklet.execute(contribution, chunkContext);

	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletNullResult() {
		mockReturnValForTaskExecution(1L);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		getCompleteTaskExecutionWithNull();
		assertThatThrownBy(() -> execute(taskLauncherTasklet, null, chunkContext))
			.isInstanceOf(UnexpectedTaskExecutionException.class)
			.hasMessage("Task returned a null exit code.");
	}

	@Test
	void taskOperationsConfiguredWithMissingPassword() {
		try {
			final ComposedTaskProperties composedTaskProperties = new ComposedTaskProperties();
			composedTaskProperties.setDataflowServerUsername("foo");

			TaskLauncherTasklet taskLauncherTasklet = new  TaskLauncherTasklet(null, null,
					this.taskExplorer, composedTaskProperties,
					TASK_NAME, new TaskProperties(), environment, mapper);
			taskLauncherTasklet.taskOperations();
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("A username may be specified only together with a password");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown");
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletIgnoreExitMessage() {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setArguments(Collections.singletonList("--ignoreExitMessage=true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isTrue();
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletIgnoreExitMessageViaProperties() {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setProperties(Collections.singletonMap("app.foo." + TaskLauncherTasklet.IGNORE_EXIT_MESSAGE_PROPERTY, "true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isTrue();
	}

	@Test
	@DirtiesContext
	void taskLauncherTaskletIgnoreExitMessageViaCommandLineOverride() {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setArguments(Collections.singletonList("--ignoreExitMessage=false"));
		taskLauncherTasklet.setProperties(Collections.singletonMap("app.foo." + TaskLauncherTasklet.IGNORE_EXIT_MESSAGE_PROPERTY, "true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);
		boolean value = chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isTrue();
		assertThat((Boolean) chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isFalse();
	}


	@Test
	void taskOperationsConfiguredWithMissingUsername() {
		try {
			final ComposedTaskProperties composedTaskProperties = new ComposedTaskProperties();
			composedTaskProperties.setDataflowServerPassword("bar");

			TaskLauncherTasklet taskLauncherTasklet = new  TaskLauncherTasklet(null, null,
					this.taskExplorer, composedTaskProperties,
					TASK_NAME, new TaskProperties(), environment, mapper);
			taskLauncherTasklet.taskOperations();
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("A password may be specified only together with a username");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown");
	}
	private void createCompleteTaskExecution(int exitCode, String... message) {
		TaskExecution taskExecution = this.taskRepository.createTaskExecution();
		this.taskRepository.completeTaskExecution(taskExecution.getExecutionId(),
				exitCode, LocalDateTime.now(),  message != null && message.length > 0 ? message[0] : "");
	}

	private void createAndStartCompleteTaskExecution(int exitCode, JobExecution jobExecution) {
		TaskExecution taskExecution = this.taskRepository.createTaskExecution();
		JdbcTaskBatchDao taskBatchDao = new JdbcTaskBatchDao(this.dataSource);
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		this.taskRepository.completeTaskExecution(taskExecution.getExecutionId(),
				exitCode, LocalDateTime.now(), "");
	}

	private TaskExecution getCompleteTaskExecutionWithNull() {
		TaskExecution taskExecution = this.taskRepository.createTaskExecution();
		taskExecutionDao.completeTaskExecution(taskExecution.getExecutionId(), null, LocalDateTime.now(),
			"hello", "goodbye");
		return taskExecution;
	}

	private TaskLauncherTasklet getTaskExecutionTasklet() {
		return getTaskExecutionTasklet(new TaskProperties());
	}

	private TaskLauncherTasklet getTaskExecutionTasklet(TaskProperties taskProperties) {
		TaskLauncherTasklet taskLauncherTasklet = new  TaskLauncherTasklet(null, null,
				this.taskExplorer, this.composedTaskProperties,
				TASK_NAME, taskProperties, environment, mapper);
		ReflectionTestUtils.setField(taskLauncherTasklet, "taskOperations", this.taskOperations);
		return taskLauncherTasklet;
	}

	private ChunkContext chunkContext ()
	{
		final long JOB_EXECUTION_ID = 123L;
		final String STEP_NAME = "myTestStep";

		JobExecution jobExecution = new JobExecution(JOB_EXECUTION_ID);
		StepExecution stepExecution = new StepExecution(STEP_NAME, jobExecution);
		StepContext stepContext = new StepContext(stepExecution);
		return new ChunkContext(stepContext);
	}

	private void mockReturnValForTaskExecution(long executionId) {
		Mockito.doReturn(new LaunchResponseResource(executionId))
				.when(this.taskOperations)
				.launch(ArgumentMatchers.anyString(),
						ArgumentMatchers.any(),
						ArgumentMatchers.any());
	}

	@Configuration
	@EnableConfigurationProperties(ComposedTaskProperties.class)
	public static class TestConfiguration {

		@Bean
		@DependsOnDatabaseInitialization
		TaskRepositoryInitializer taskRepositoryInitializer() {
			return new TaskRepositoryInitializer(new TaskProperties());
		}

		@Bean
		TaskExecutionDao taskExecutionDao(DataSource dataSource) {
			return new JdbcTaskExecutionDao(dataSource);
		}

	}

	private static class TestTaskLauncherTasklet extends TaskLauncherTasklet {
		public TestTaskLauncherTasklet(
				ClientRegistrationRepository clientRegistrations,
				OAuth2AccessTokenResponseClient<OAuth2ClientCredentialsGrantRequest> clientCredentialsTokenResponseClient,
				TaskExplorer taskExplorer,
				ComposedTaskProperties composedTaskProperties, String taskName,
				TaskProperties taskProperties,
				Environment environment,
				ObjectMapper mapper) {
			super(clientRegistrations, clientCredentialsTokenResponseClient,taskExplorer,composedTaskProperties,taskName,taskProperties, environment, mapper);
		}

		@Override
		protected DataFlowOperations dataFlowOperations() {
			DataFlowOperations dataFlowOperations = Mockito.mock(DataFlowOperations.class);
			Mockito.doReturn(null)
					.when(dataFlowOperations)
					.taskOperations();
			return dataFlowOperations;
		}
	}
}
