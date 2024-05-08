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
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
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
import org.springframework.cloud.dataflow.schema.AppBootSchemaVersion;
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
public class DefaultTaskJobServiceTests {

	private static final String SAVE_JOB_EXECUTION = "INSERT INTO BOOT3_BATCH_JOB_EXECUTION(JOB_EXECUTION_ID, " +
		"JOB_INSTANCE_ID, START_TIME, END_TIME, STATUS, EXIT_CODE, EXIT_MESSAGE, VERSION, CREATE_TIME, LAST_UPDATED) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String SAVE_JOB_EXECUTION_PARAM = "INSERT INTO BOOT3_BATCH_JOB_EXECUTION_PARAMS (" +
		"job_execution_id, parameter_name, parameter_type, parameter_value, identifying) " +
		"VALUES (?, ?, ?, ?, ?)";

	private final static String BASE_JOB_NAME = "myJob";

	private final static String JOB_NAME_ORIG = BASE_JOB_NAME + "_ORIG";

	private static long jobInstanceCount = 0;

	private static long boot3JobInstanceCount = 0;

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

	@BeforeEach
	public void setup() {
		Map<String, JobParameter> jobParameterMap = new HashMap<>();
		jobParameterMap.put("identifying.param", new JobParameter("testparam"));
		this.jobParameters = new JobParameters(jobParameterMap);

		this.jdbcTemplate = new JdbcTemplate(this.dataSource);
		resetTaskTables("TASK_");
		initializeSuccessfulRegistry(this.appRegistry);
		resetTaskTables("BOOT3_TASK_");

		reset(this.taskLauncher);
		when(this.taskLauncher.launch(any())).thenReturn("1234");
		clearLaunchers();
	}

	private void resetTaskTables(String prefix) {
		this.jdbcTemplate.execute("DELETE FROM " + prefix  + "EXECUTION_PARAMS");
		this.jdbcTemplate.execute("DELETE FROM " + prefix  + "TASK_BATCH");
		this.jdbcTemplate.execute("DELETE FROM " + prefix  + "EXECUTION_METADATA");
		this.jdbcTemplate.execute("DELETE FROM " + prefix  + "EXECUTION;");
		this.jdbcTemplate.execute("ALTER SEQUENCE " + prefix  + "EXECUTION_METADATA_SEQ RESTART WITH 50");
		this.jdbcTemplate.execute("ALTER SEQUENCE " + prefix  + "SEQ RESTART WITH 1");
		this.jdbcTemplate.execute("INSERT INTO " + prefix  + "EXECUTION (TASK_EXECUTION_ID, TASK_NAME) VALUES (0, 'myTask_ORIG');");
	}

	@Test
	public void testRestart() throws Exception {
		createBaseLaunchers();
		initializeJobs(true);

		this.taskJobService.restartJobExecution(jobInstanceCount, SchemaVersionTarget.defaultTarget().getName());
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);

		assertTrue(appDeploymentRequest.getCommandlineArguments().contains("identifying.param(string)=testparam"));
	}

	@Test
	public void testRestartBoot3() throws Exception {
		SchemaVersionTarget schemaVersionTarget = new SchemaVersionTarget("boot3", AppBootSchemaVersion.BOOT3,
			"BOOT3_TASK_", "BOOT3_BATCH_", "H2");
		createBaseLaunchers();
		initializeJobs(true, schemaVersionTarget);
		this.taskJobService.restartJobExecution(boot3JobInstanceCount,
			SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3).getName());
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		assertTrue(appDeploymentRequest.getCommandlineArguments().contains("identifying.param=testparm,java.lang.String"));
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
		initializeJobs(insertTaskExecutionMetadata,
			new SchemaVersionTarget("boot2", AppBootSchemaVersion.BOOT2, "TASK_",
				"BATCH_", "H2"));
	}
	private void initializeJobs(boolean insertTaskExecutionMetadata, SchemaVersionTarget schemaVersionTarget) {
		String definitionName = (AppBootSchemaVersion.BOOT3.equals(schemaVersionTarget.getSchemaVersion())) ?
			"some-name-boot3" : "some-name";
		this.taskDefinitionRepository.save(new TaskDefinition(JOB_NAME_ORIG + jobInstanceCount, definitionName  ));
		JobRepository jobRepository = jobRepositoryContainer.get(schemaVersionTarget.getName());
		TaskBatchDao taskBatchDao = taskBatchDaoContainer.get(schemaVersionTarget.getName());
		TaskExecutionDao taskExecutionDao = taskExecutionDaoContainer.get(schemaVersionTarget.getName());
		createSampleJob(
				jobRepository,
				taskBatchDao,
				taskExecutionDao,
				JOB_NAME_ORIG + jobInstanceCount,
				BatchStatus.FAILED,
				insertTaskExecutionMetadata,
				schemaVersionTarget
		);
		if(AppBootSchemaVersion.BOOT2.equals(schemaVersionTarget.getSchemaVersion())) {
			jobInstanceCount++;
		}
		else {
			boot3JobInstanceCount++;
		}

	}

	private void createSampleJob(
			JobRepository jobRepository,
			TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao,
			String jobName,
			BatchStatus status,
			boolean insertTaskExecutionMetadata,
			SchemaVersionTarget schemaVersionTarget
	) {
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());

		TaskExecution taskExecution = taskExecutionDao.createTaskExecution(jobName, new Date(), Collections.emptyList(), null);
		JobExecution jobExecution;
		JdbcTemplate template = new JdbcTemplate(this.dataSource);

		if (insertTaskExecutionMetadata) {
			template.execute(String.format("INSERT INTO " + schemaVersionTarget.getTaskPrefix() + "EXECUTION_METADATA (ID, TASK_EXECUTION_ID, TASK_EXECUTION_MANIFEST) VALUES (%s, %s, '{\"taskDeploymentRequest\":{\"definition\":{\"name\":\"bd0917a\",\"properties\":{\"spring.datasource.username\":\"root\",\"spring.cloud.task.name\":\"bd0917a\",\"spring.datasource.url\":\"jdbc:mariadb://localhost:3306/task\",\"spring.datasource.driverClassName\":\"org.mariadb.jdbc.Driver\",\"spring.datasource.password\":\"password\"}},\"resource\":\"file:/Users/glennrenfro/tmp/batchdemo-0.0.1-SNAPSHOT.jar\",\"deploymentProperties\":{},\"commandlineArguments\":[\"run.id_long=1\",\"--spring.cloud.task.executionid=201\"]},\"platformName\":\"demo\"}')", taskExecution.getExecutionId(), taskExecution.getExecutionId()));
		}
		if(AppBootSchemaVersion.BOOT3.equals(schemaVersionTarget.getSchemaVersion())) {
			jobExecution = new JobExecution(instance, 1L, this.jobParameters, "foo");
			jobExecution.setCreateTime(new Date());
			jobExecution.setVersion(1);
			Object[] jobExecutionParameters = new Object[] { 1, 1, new Date(), new Date(),
				BatchStatus.COMPLETED, ExitStatus.COMPLETED,
				ExitStatus.COMPLETED.getExitDescription(), 1, new Date(), new Date() };
			Object[] jobExecutionParmParameters = new Object[] { 1,  "identifying.param", "java.lang.String", "testparm", "Y"};
			this.jdbcTemplate.update(SAVE_JOB_EXECUTION, jobExecutionParameters,
				new int[] { Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR,
					Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP });
			this.jdbcTemplate.update(SAVE_JOB_EXECUTION_PARAM, jobExecutionParmParameters, new int[] { Types.BIGINT,
					Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CHAR});
		} else {
			jobExecution = jobRepository.createJobExecution(instance,
				this.jobParameters, null);
				StepExecution stepExecution = new StepExecution("foo", jobExecution, 1L);
				stepExecution.setId(null);
				jobRepository.add(stepExecution);
		}
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(new Date());
		jobRepository.update(jobExecution);
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
		when(appRegistry.find(eq("some-name-boot3"), any(ApplicationType.class))).thenReturn(
			new AppRegistration("some-name-boot3", ApplicationType.task, "", URI.create("https://helloworld"), URI.create("https://helloworld"), AppBootSchemaVersion.fromBootVersion("3")));
		try {
			when(appRegistry.getAppResource(any())).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task"));
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid File Resource Specified", e);
		}
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}
}
