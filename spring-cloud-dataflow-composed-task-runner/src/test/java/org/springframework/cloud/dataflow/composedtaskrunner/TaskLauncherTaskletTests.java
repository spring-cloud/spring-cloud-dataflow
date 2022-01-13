/*
 * Copyright 2017-2021 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.AssertionsForClassTypes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.properties.ComposedTaskProperties;
import org.springframework.cloud.dataflow.composedtaskrunner.support.ComposedTaskException;
import org.springframework.cloud.dataflow.composedtaskrunner.support.TaskExecutionTimeoutException;
import org.springframework.cloud.dataflow.rest.client.DataFlowClientException;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.TaskOperations;
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
import org.springframework.hateoas.Link;
import org.springframework.hateoas.mediatype.vnderrors.VndErrors;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;


/**
 * @author Glenn Renfro
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes={EmbeddedDataSourceConfiguration.class,
		org.springframework.cloud.dataflow.composedtaskrunner.TaskLauncherTaskletTests.TestConfiguration.class})
public class TaskLauncherTaskletTests {

	private static final String TASK_NAME = "testTask1_0";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private ComposedTaskProperties composedTaskProperties;

	@Autowired
	private TaskRepositoryInitializer taskRepositoryInitializer;

	@Autowired
	private JdbcTaskExecutionDao taskExecutionDao;

	private TaskOperations taskOperations;

	private TaskRepository taskRepository;

	private TaskExplorer taskExplorer;


	@BeforeEach
	public void setup() throws Exception{
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
	public void testTaskLauncherTasklet() throws Exception{
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
	public void testInvalidTaskOperations() throws Exception{
		TaskLauncherTasklet taskLauncherTasklet = new TestTaskLauncherTasklet(null, null,
				this.taskExplorer, this.composedTaskProperties,
				TASK_NAME, new TaskProperties());
		Exception exception = assertThrows(ComposedTaskException.class, () -> {
			execute(taskLauncherTasklet, null, chunkContext());
		});
		AssertionsForClassTypes.assertThat(exception.getMessage()).isEqualTo(
				"Unable to connect to Data Flow Server to execute task operations. " +
						"Verify that Data Flow Server's tasks/definitions endpoint can be accessed.");
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletWithTaskExecutionId() throws Exception{
		TaskLauncherTasklet taskLauncherTasklet = prepTaskLauncherTests();

		TaskProperties taskProperties = new TaskProperties();
		taskProperties.setExecutionid(88L);
		mockReturnValForTaskExecution(2L);
		ChunkContext chunkContext = chunkContext();
		createCompleteTaskExecution(0);
		taskLauncherTasklet = getTaskExecutionTasklet(taskProperties);
		taskLauncherTasklet.setArguments(null);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(2L);
		assertThat(((List) chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-arguments")).get(0)).isEqualTo("--spring.cloud.task.parent-execution-id=88");
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletWithTaskExecutionIdWithPreviousParentID() throws Exception{

		TaskLauncherTasklet taskLauncherTasklet = prepTaskLauncherTests();
		TaskProperties taskProperties = new TaskProperties();
		taskProperties.setExecutionid(88L);
		mockReturnValForTaskExecution(2L);
		ChunkContext chunkContext = chunkContext();
		createCompleteTaskExecution(0);
		chunkContext.getStepContext()
				.getStepExecution().getExecutionContext().put("task-arguments", new ArrayList<String>());
		((List)chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-arguments")).add("--spring.cloud.task.parent-execution-id=84");
		taskLauncherTasklet = getTaskExecutionTasklet(taskProperties);
		taskLauncherTasklet.setArguments(null);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(2L);
		assertThat(((List) chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-arguments")).get(0)).isEqualTo("--spring.cloud.task.parent-execution-id=88");
	}

	private TaskLauncherTasklet prepTaskLauncherTests() throws Exception{
		createCompleteTaskExecution(0);
		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);
		assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-arguments")).isNull();
		return taskLauncherTasklet;
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletTimeout() {
		mockReturnValForTaskExecution(1L);
		this.composedTaskProperties.setMaxWaitTime(500);
		this.composedTaskProperties.setIntervalTimeBetweenChecks(1000);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		Throwable exception = assertThrows(TaskExecutionTimeoutException.class, () -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo("Timeout occurred while " +
				"processing task with Execution Id 1");
	}

	@Test
	@DirtiesContext
	public void testInvalidTaskName() {
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
		Throwable exception = assertThrows(DataFlowClientException.class,
				() -> taskLauncherTasklet.execute(null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
	}

	@Test
	@DirtiesContext
	public void testNoDataFlowServer() {
		final String ERROR_MESSAGE =
				"I/O error on GET request for \"http://localhost:9393\": Connection refused; nested exception is java.net.ConnectException: Connection refused";
		Mockito.doThrow(new ResourceAccessException(ERROR_MESSAGE))
				.when(this.taskOperations).launch(ArgumentMatchers.anyString(),
				ArgumentMatchers.any(),
				ArgumentMatchers.any());
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		Throwable exception = assertThrows(ResourceAccessException.class,
				() -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo(ERROR_MESSAGE);
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletFailure() {
		mockReturnValForTaskExecution(1L);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		createCompleteTaskExecution(1);
		Throwable exception = assertThrows(UnexpectedJobExecutionException.class,
				() -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo("Task returned a non zero exit code.");
	}

	private RepeatStatus execute(TaskLauncherTasklet taskLauncherTasklet, StepContribution contribution,
			ChunkContext chunkContext)  throws Exception{
		RepeatStatus status = taskLauncherTasklet.execute(contribution, chunkContext);
		if (!status.isContinuable()) {
			throw new IllegalStateException("Expected continuable status for the first execution.");
		}
		return taskLauncherTasklet.execute(contribution, chunkContext);

	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletNullResult() throws Exception {
		boolean isException = false;
		mockReturnValForTaskExecution(1L);
		TaskLauncherTasklet taskLauncherTasklet = getTaskExecutionTasklet();
		ChunkContext chunkContext = chunkContext();
		getCompleteTaskExecutionWithNull();
		Throwable exception = assertThrows(UnexpectedJobExecutionException.class,
				() -> execute(taskLauncherTasklet, null, chunkContext));
		Assertions.assertThat(exception.getMessage()).isEqualTo("Task returned a null exit code.");
	}

	@Test
	public void testTaskOperationsConfiguredWithMissingPassword() {
		try {
			final ComposedTaskProperties composedTaskProperties = new ComposedTaskProperties();
			composedTaskProperties.setDataflowServerUsername("foo");

			TaskLauncherTasklet taskLauncherTasklet = new  TaskLauncherTasklet(null, null,
					this.taskExplorer, composedTaskProperties,
					TASK_NAME, new TaskProperties());
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
	public void testTaskLauncherTaskletIgnoreExitMessage() throws Exception {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setArguments(Collections.singletonList("--ignoreExitMessage=true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		Assertions.assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);
		Assertions.assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isTrue();
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletIgnoreExitMessageViaProperties() throws Exception {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setProperties(Collections.singletonMap("app.foo." + TaskLauncherTasklet.IGNORE_EXIT_MESSAGE_PROPERTY, "true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		Assertions.assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);
		Assertions.assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isTrue();
	}

	@Test
	@DirtiesContext
	public void testTaskLauncherTaskletIgnoreExitMessageViaCommandLineOverride() throws Exception {
		createCompleteTaskExecution(0);

		TaskLauncherTasklet taskLauncherTasklet =
				getTaskExecutionTasklet();
		taskLauncherTasklet.setArguments(Collections.singletonList("--ignoreExitMessage=false"));
		taskLauncherTasklet.setProperties(Collections.singletonMap("app.foo." + TaskLauncherTasklet.IGNORE_EXIT_MESSAGE_PROPERTY, "true"));
		ChunkContext chunkContext = chunkContext();
		mockReturnValForTaskExecution(1L);
		execute(taskLauncherTasklet, null, chunkContext);
		Assertions.assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get("task-execution-id")).isEqualTo(1L);
		boolean value = chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE);
		Assertions.assertThat(chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.containsKey(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isTrue();
		Assertions.assertThat((Boolean)chunkContext.getStepContext()
				.getStepExecution().getExecutionContext()
				.get(TaskLauncherTasklet.IGNORE_EXIT_MESSAGE)).isFalse();
	}


	@Test
	public void testTaskOperationsConfiguredWithMissingUsername() {
		try {
			final ComposedTaskProperties composedTaskProperties = new ComposedTaskProperties();
			composedTaskProperties.setDataflowServerPassword("bar");

			TaskLauncherTasklet taskLauncherTasklet = new  TaskLauncherTasklet(null, null,
					this.taskExplorer, composedTaskProperties,
					TASK_NAME, new TaskProperties());
			taskLauncherTasklet.taskOperations();
		}
		catch (IllegalArgumentException e) {
			assertThat(e.getMessage()).isEqualTo("A password may be specified only together with a username");
			return;
		}
		fail("Expected an IllegalArgumentException to be thrown");
	}

	private void createCompleteTaskExecution(int exitCode) {
		TaskExecution taskExecution = this.taskRepository.createTaskExecution();
		this.taskRepository.completeTaskExecution(taskExecution.getExecutionId(),
				exitCode, new Date(), "");
	}

	private TaskExecution getCompleteTaskExecutionWithNull() {
		TaskExecution taskExecution = this.taskRepository.createTaskExecution();
		taskExecutionDao.completeTaskExecution(taskExecution.getExecutionId(), null, new Date(), "hello", "goodbye");
		return taskExecution;
	}

	private TaskLauncherTasklet getTaskExecutionTasklet() {
		return getTaskExecutionTasklet(new TaskProperties());
	}

	private TaskLauncherTasklet getTaskExecutionTasklet(TaskProperties taskProperties) {
		TaskLauncherTasklet taskLauncherTasklet = new  TaskLauncherTasklet(null, null,
				this.taskExplorer, this.composedTaskProperties,
				TASK_NAME, taskProperties);
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
		Mockito.doReturn(executionId)
				.when(this.taskOperations)
				.launch(ArgumentMatchers.anyString(),
						ArgumentMatchers.any(),
						ArgumentMatchers.any());
	}

	@Configuration
	@EnableBatchProcessing
	@EnableConfigurationProperties(ComposedTaskProperties.class)
	public static class TestConfiguration {

		@Bean
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
				TaskProperties taskProperties) {
			super(clientRegistrations, clientCredentialsTokenResponseClient,taskExplorer,composedTaskProperties,taskName,taskProperties);
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
