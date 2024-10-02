/*
 * Copyright 2019-2023 the original author or authors.
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

import java.net.MalformedURLException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.task.TaskDefinitionReader;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.core.io.FileUrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {
		TaskServiceDependencies.class,
		JobDependencies.class,
		BatchProperties.class
}, properties = {
		"spring.main.allow-bean-definition-overriding=true"}
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class DefaultTaskJobServiceTests {

	private final static String BASE_JOB_NAME = "myJob";

	private final static String JOB_NAME_ORIG = BASE_JOB_NAME + "_ORIG";

	private static long jobInstanceCount = 0;

	@Autowired
	TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	LauncherRepository launcherRepository;

	@Autowired
	TaskLauncher taskLauncher;

	@Autowired
	AppRegistryService appRegistry;

	@Autowired
	DataSource dataSource;

	JdbcTemplate jdbcTemplate;

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Autowired
	JobRepository jobRepository;

	@Autowired
	TaskBatchDao taskBatchDao;

	@Autowired
	TaskExecutionDao taskExecutionDao;

	@Autowired
	TaskJobService taskJobService;

	private JobParameters jobParameters;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;

	@BeforeEach
	void setup() {
		Map<String, JobParameter<?>> jobParameterMap = new HashMap<>();
		jobParameterMap.put("identifying.param", new JobParameter("testparam", String.class));
		this.jobParameters = new JobParameters(jobParameterMap);

		this.jdbcTemplate = new JdbcTemplate(this.dataSource);
		resetTables("TASK_", "BATCH_");
		initializeSuccessfulRegistry(this.appRegistry);
		reset(this.taskLauncher);
		when(this.taskLauncher.launch(any())).thenReturn("1234");
		clearLaunchers();
	}

	private void resetTables(String taskPrefix, String batchPrefix) {
		deleteTable(taskPrefix, "EXECUTION_PARAMS");
		deleteTable(taskPrefix, "TASK_BATCH");
		deleteTable(taskPrefix,  "EXECUTION_METADATA");
		deleteTable(taskPrefix, "EXECUTION");
		this.jdbcTemplate.execute(String.format("ALTER SEQUENCE %s%s", taskPrefix, "EXECUTION_METADATA_SEQ RESTART WITH 50"));
		this.jdbcTemplate.execute(String.format("ALTER SEQUENCE %s%s", taskPrefix, "SEQ RESTART WITH 1"));
		deleteTable(batchPrefix, "STEP_EXECUTION_CONTEXT");
		deleteTable(batchPrefix, "STEP_EXECUTION");
		deleteTable(batchPrefix, "JOB_EXECUTION_CONTEXT");
		deleteTable(batchPrefix, "JOB_EXECUTION_PARAMS");
		deleteTable(batchPrefix, "JOB_EXECUTION");
		deleteTable(batchPrefix, "JOB_INSTANCE");
		this.jdbcTemplate.execute(String.format("INSERT INTO %s%s", taskPrefix, "EXECUTION (TASK_EXECUTION_ID, TASK_NAME) VALUES (0, 'myTask_ORIG');"));
	}

	private void deleteTable(String prefix, String tableName) {
		this.jdbcTemplate.execute(String.format("DELETE FROM %s%s", prefix, tableName));
	}

	@Test
	void restart() throws Exception {
		createBaseLaunchers();
		initializeJobs(true);

		this.taskJobService.restartJobExecution(jobInstanceCount);
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		assertThat(appDeploymentRequest.getCommandlineArguments()).contains("identifying.param=testparam,java.lang.String,true");
	}

	@Test
	void restartWithJsonParameters() throws Exception {
		createBaseLaunchers();
		initializeJobs(true);

		this.taskJobService.restartJobExecution(jobInstanceCount, true);
		ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		assertThat(appDeploymentRequest.getCommandlineArguments()).contains("identifying.param={\"value\":\"testparam\",\"type\":\"java.lang.String\",\"identifying\":\"true\"}");
	}

	@Test
	void restartNoPlatform()
			throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		createBaseLaunchers();
		initializeJobs(false);
		assertThatThrownBy(() -> {
			this.taskJobService.restartJobExecution(jobInstanceCount);
		}).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("Did not find platform for taskName=[myJob_ORIG");
	}

	@Test
	void restartOnePlatform() throws Exception {
		this.launcherRepository.save(new Launcher("demo", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, this.taskLauncher));

		initializeJobs(false);
		this.taskJobService.restartJobExecution(jobInstanceCount);
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		assertThat(appDeploymentRequest.getCommandlineArguments()).contains("identifying.param=testparam,java.lang.String,true");
	}

	private void initializeJobs(boolean insertTaskExecutionMetadata)
		throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		String definitionName =  "some-name";
		this.taskDefinitionRepository.save(new TaskDefinition(JOB_NAME_ORIG + jobInstanceCount, definitionName  ));
		createSampleJob(
				jobRepository,
				taskBatchDao,
				taskExecutionDao,
				JOB_NAME_ORIG + jobInstanceCount,
				BatchStatus.FAILED,
				insertTaskExecutionMetadata
		);
			jobInstanceCount++;
	}

	private Pair<TaskExecution, JobExecution> createSampleJob(
			JobRepository jobRepository,
			TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao,
			String jobName,
			BatchStatus status,
			boolean insertTaskExecutionMetadata
	) throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobRestartException {
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());

		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, LocalDateTime.now(), Collections.emptyList(), null);
		JobExecution jobExecution;
		JdbcTemplate template = new JdbcTemplate(this.dataSource);

		if (insertTaskExecutionMetadata) {
			template.execute(String.format("INSERT INTO TASK_EXECUTION_METADATA (ID, TASK_EXECUTION_ID, TASK_EXECUTION_MANIFEST) VALUES (%s, %s, '{\"taskDeploymentRequest\":{\"definition\":{\"name\":\"bd0917a\",\"properties\":{\"spring.datasource.username\":\"root\",\"spring.cloud.task.name\":\"bd0917a\",\"spring.datasource.url\":\"jdbc:mariadb://localhost:3306/task\",\"spring.datasource.driverClassName\":\"org.mariadb.jdbc.Driver\",\"spring.datasource.password\":\"password\"}},\"resource\":\"file:/Users/glennrenfro/tmp/batchdemo-0.0.1-SNAPSHOT.jar\",\"deploymentProperties\":{},\"commandlineArguments\":[\"run.id_long=1\",\"--spring.cloud.task.executionid=201\"]},\"platformName\":\"demo\"}')", taskExecution.getExecutionId(), taskExecution.getExecutionId()));
		}
		jobExecution = jobRepository.createJobExecution(jobName,
			this.jobParameters);
		StepExecution stepExecution = new StepExecution("foo", jobExecution, 1L);
		stepExecution.setId(null);
		jobRepository.add(stepExecution);
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(LocalDateTime.now());
		jobRepository.update(jobExecution);
		return Pair.of(taskExecution, jobExecution);
	}

	private void clearLaunchers() {
		List<Launcher> launchers = new ArrayList<>();
		this.launcherRepository.findAll().forEach(launchers::add);
		this.launcherRepository.deleteAll(launchers);
	}

	private void createBaseLaunchers() {
		// not adding platform name as default as we want to check that this only one
		// gets replaced
		this.launcherRepository.save(new Launcher("fakeplatformname", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, this.taskLauncher));
		this.launcherRepository.save(new Launcher("demo", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, this.taskLauncher));
	}

	private static void initializeSuccessfulRegistry(AppRegistryService appRegistry) {
		when(appRegistry.find(eq("some-name"), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		try {
			when(appRegistry.getAppResource(any())).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task"));
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid File Resource Specified", e);
		}
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}
}
