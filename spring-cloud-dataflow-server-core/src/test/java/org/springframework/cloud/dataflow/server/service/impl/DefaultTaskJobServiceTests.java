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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.batch.BatchProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.configuration.JobDependencies;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.JobRepositoryContainer;
import org.springframework.cloud.dataflow.server.repository.TaskBatchDaoContainer;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.repository.TaskExecutionDaoContainer;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.task.batch.listener.TaskBatchDao;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.dao.TaskExecutionDao;
import org.springframework.core.io.FileUrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		TaskServiceDependencies.class,
		JobDependencies.class,
		BatchProperties.class
}, properties = {
		"spring.main.allow-bean-definition-overriding=true"}
)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class DefaultTaskJobServiceTests {

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

	@Autowired
	DataSourceProperties dataSourceProperties;

	@Autowired
	JobRepositoryContainer jobRepositoryContainer;

	@Autowired
	TaskBatchDaoContainer taskBatchDaoContainer;

	@Autowired
	TaskExecutionDaoContainer taskExecutionDaoContainer;

	@Autowired
	TaskJobService taskJobService;

	@Autowired
	AggregateExecutionSupport aggregateExecutionSupport;

	private JobParameters jobParameters;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;

	@Before
	public void setup() {
		Map<String, JobParameter> jobParameterMap = new HashMap<>();
		jobParameterMap.put("identifying.param", new JobParameter("testparam"));
		this.jobParameters = new JobParameters(jobParameterMap);

		JdbcTemplate template = new JdbcTemplate(this.dataSource);
		template.execute("DELETE FROM TASK_EXECUTION_PARAMS");
		template.execute("DELETE FROM TASK_TASK_BATCH");
		template.execute("DELETE FROM task_execution_metadata");
		template.execute("DELETE FROM TASK_EXECUTION;");
		template.execute("ALTER SEQUENCE task_execution_metadata_seq RESTART WITH 50");
		template.execute("ALTER SEQUENCE task_seq RESTART WITH 1");
		initializeSuccessfulRegistry(this.appRegistry);
		template.execute("INSERT INTO TASK_EXECUTION (TASK_EXECUTION_ID, TASK_NAME) VALUES (0, 'myTask_ORIG');");
		reset(this.taskLauncher);
		when(this.taskLauncher.launch(any())).thenReturn("1234");
		clearLaunchers();
	}

	@Test
	public void testRestart() throws Exception {
		createBaseLaunchers();
		initializeJobs(true);

		this.taskJobService.restartJobExecution(jobInstanceCount, SchemaVersionTarget.defaultTarget().getName());
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		appDeploymentRequest.getCommandlineArguments().contains("--spring.cloud.data.flow.platformname=demo");
		assertTrue(appDeploymentRequest.getCommandlineArguments().contains("identifying.param(string)=testparam"));
	}

	@Test
	public void testRestartNoPlatform() {
		createBaseLaunchers();
		initializeJobs(false);
		Exception exception = assertThrows(IllegalStateException.class, () -> {
			this.taskJobService.restartJobExecution(jobInstanceCount, SchemaVersionTarget.defaultTarget().getName());
		});
		assertTrue(exception.getMessage().contains("Did not find platform for taskName=[myJob_ORIG"));
	}

	@Test
	public void testRestartOnePlatform() throws Exception {
		this.launcherRepository.save(new Launcher("demo", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, this.taskLauncher));

		initializeJobs(false);
		this.taskJobService.restartJobExecution(jobInstanceCount, SchemaVersionTarget.defaultTarget().getName());
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		assertTrue(appDeploymentRequest.getCommandlineArguments().contains("identifying.param(string)=testparam"));
	}

	private void initializeJobs(boolean insertTaskExecutionMetadata) {
		this.taskDefinitionRepository.save(new TaskDefinition(JOB_NAME_ORIG + jobInstanceCount, "some-name"));
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget("some-name", taskDefinitionReader);
		JobRepository jobRepository = jobRepositoryContainer.get(schemaVersionTarget.getName());
		TaskBatchDao taskBatchDao = taskBatchDaoContainer.get(schemaVersionTarget.getName());
		TaskExecutionDao taskExecutionDao = taskExecutionDaoContainer.get(schemaVersionTarget.getName());
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

	private void createSampleJob(
			JobRepository jobRepository,
			TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao,
			String jobName,
			BatchStatus status,
			boolean insertTaskExecutionMetadata
	) {
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());

		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, new Date(), Collections.emptyList(), null);
		JobExecution jobExecution;
		JdbcTemplate template = new JdbcTemplate(this.dataSource);

		if (insertTaskExecutionMetadata) {
			template.execute(String.format("INSERT INTO task_execution_metadata (id, task_execution_id, task_execution_manifest) VALUES (%s, %s, '{\"taskDeploymentRequest\":{\"definition\":{\"name\":\"bd0917a\",\"properties\":{\"spring.datasource.username\":\"root\",\"spring.cloud.task.name\":\"bd0917a\",\"spring.datasource.url\":\"jdbc:mariadb://localhost:3306/task\",\"spring.datasource.driverClassName\":\"org.mariadb.jdbc.Driver\",\"spring.datasource.password\":\"password\"}},\"resource\":\"file:/Users/glennrenfro/tmp/batchdemo-0.0.1-SNAPSHOT.jar\",\"deploymentProperties\":{},\"commandlineArguments\":[\"run.id_long=1\",\"--spring.cloud.task.executionid=201\"]},\"platformName\":\"demo\"}')", taskExecution.getExecutionId(), taskExecution.getExecutionId()));
		}
		jobExecution = jobRepository.createJobExecution(instance,
				this.jobParameters, null);
		StepExecution stepExecution = new StepExecution("foo", jobExecution, 1L);
		stepExecution.setId(null);
		jobRepository.add(stepExecution);
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(new Date());
		jobRepository.update(jobExecution);
	}

	private void clearLaunchers() {
		List<Launcher> launchers = new ArrayList<>();
		this.launcherRepository.findAll().forEach(launcher1 -> launchers.add(launcher1));
		launchers.stream().forEach(launcher -> this.launcherRepository.delete(launcher));
	}

	private void createBaseLaunchers() {
		// not adding platform name as default as we want to check that this only one
		// gets replaced
		this.launcherRepository.save(new Launcher("fakeplatformname", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, this.taskLauncher));
		this.launcherRepository.save(new Launcher("demo", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, this.taskLauncher));
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
}
