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
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.core.TaskPlatformFactory;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.util.Pair;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
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

	@Autowired
	AggregateTaskExplorer aggregateTaskExplorer;

	private JobParameters jobParameters;

	@Autowired
	TaskDefinitionReader taskDefinitionReader;

	@BeforeEach
	public void setup() {
		Map<String, JobParameter> jobParameterMap = new HashMap<>();
		jobParameterMap.put("identifying.param", new JobParameter("testparam"));
		this.jobParameters = new JobParameters(jobParameterMap);

		this.jdbcTemplate = new JdbcTemplate(this.dataSource);
		resetTables("TASK_", "BATCH_");
		initializeSuccessfulRegistry(this.appRegistry);
		resetTables("BOOT3_TASK_", "BOOT3_BATCH_");

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
	public void testRestart() throws Exception {
		createBaseLaunchers();
		Pair<TaskExecution, JobExecution> executionPair = initializeJobs(true, SchemaVersionTarget.defaultTarget());
		this.taskJobService.restartJobExecution(executionPair.getSecond().getId(), SchemaVersionTarget.defaultTarget().getName());
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);

		assertThat(appDeploymentRequest.getCommandlineArguments()).contains("identifying.param(string)=testparam");
	}

	@Test
	public void testRestartBoot3() throws Exception {
		SchemaVersionTarget schemaVersionTarget = new SchemaVersionTarget("boot3", AppBootSchemaVersion.BOOT3,"BOOT3_TASK_", "BOOT3_BATCH_", "H2");
		createBaseLaunchers();
		Pair<TaskExecution, JobExecution> executionPair = initializeJobs(true, schemaVersionTarget);
		this.taskJobService.restartJobExecution(executionPair.getSecond().getId(), schemaVersionTarget.getName());
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		assertThat(appDeploymentRequest.getCommandlineArguments()).contains("identifying.param=testparm,java.lang.String");
	}

	@Test
	public void testRestartNoPlatform() {
		createBaseLaunchers();
		Pair<TaskExecution, JobExecution> executionPair = initializeJobs(false, SchemaVersionTarget.defaultTarget());
		Exception exception = assertThrows(IllegalStateException.class, () -> {
			JobExecution jobExecution = executionPair.getSecond();
			this.taskJobService.restartJobExecution(jobExecution.getId(), SchemaVersionTarget.defaultTarget().getName());
		});
		TaskExecution execution = executionPair.getFirst();
		assertThat(exception.getMessage()).contains("Did not find platform for taskName=[" + execution.getTaskName() + "]");
	}

	@Test
	public void testRestartOnePlatform() throws Exception {
		this.launcherRepository.save(new Launcher("demo", TaskPlatformFactory.LOCAL_PLATFORM_TYPE, this.taskLauncher));

		Pair<TaskExecution, JobExecution> executionPair = initializeJobs(false);
		this.taskJobService.restartJobExecution(executionPair.getSecond().getId(), SchemaVersionTarget.defaultTarget().getName());
		final ArgumentCaptor<AppDeploymentRequest> argument = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		verify(this.taskLauncher, times(1)).launch(argument.capture());
		AppDeploymentRequest appDeploymentRequest = argument.getAllValues().get(0);
		assertThat(appDeploymentRequest.getCommandlineArguments()).contains("identifying.param(string)=testparam");
	}

	@Test
	public void populateCtrStatus() {
		Pair<TaskExecution, JobExecution> ctr = initialiseJob(true, SchemaVersionTarget.defaultTarget(), "a && b",
				"a-b");
		initialiseJob(true, SchemaVersionTarget.defaultTarget(), "a", "a", ctr.getFirst().getExecutionId());
		initialiseJob(true, SchemaVersionTarget.createDefault(AppBootSchemaVersion.BOOT3), "b", "b",
				ctr.getFirst().getExecutionId());
		Page<AggregateTaskExecution> page = aggregateTaskExplorer.findAll(Pageable.ofSize(100));
		assertThat(page.getContent().size()).isEqualTo(5);
		AggregateTaskExecution ctrTask = page.stream()
			.filter(aggregateTaskExecution -> aggregateTaskExecution.getTaskName().equals("a-b"))
			.findFirst()
			.orElse(null);
		assertThat(ctrTask).isNotNull();
		assertThat(ctrTask.getCtrTaskStatus()).isNull();
		taskJobService.populateComposeTaskRunnerStatus(page.getContent());
		assertThat(page.stream()).anyMatch(aggregateTaskExecution -> aggregateTaskExecution.getExecutionId() == ctrTask.getExecutionId());
		assertThat(ctrTask.getCtrTaskStatus()).isEqualTo("FAILED");
	}
	private Pair<TaskExecution, JobExecution> initializeJobs(boolean insertTaskExecutionMetadata) {
		return initializeJobs(insertTaskExecutionMetadata,
			new SchemaVersionTarget("boot2", AppBootSchemaVersion.BOOT2, "TASK_",
				"BATCH_", "H2"));
	}
	private Pair<TaskExecution, JobExecution> initializeJobs(boolean insertTaskExecutionMetadata, SchemaVersionTarget schemaVersionTarget) {
		String definitionName = (AppBootSchemaVersion.BOOT3.equals(schemaVersionTarget.getSchemaVersion())) ?
			"some-name-boot3" : "some-name";
		String definition = JOB_NAME_ORIG + jobInstanceCount;
		return initialiseJob(insertTaskExecutionMetadata, schemaVersionTarget, definition, definitionName);

	}

	private Pair<TaskExecution, JobExecution> initialiseJob(boolean insertTaskExecutionMetadata,
			SchemaVersionTarget schemaVersionTarget, String definition, String definitionName) {
		return initialiseJob(insertTaskExecutionMetadata, schemaVersionTarget, definition, definitionName, null);
	}

	private Pair<TaskExecution, JobExecution> initialiseJob(boolean insertTaskExecutionMetadata,
			SchemaVersionTarget schemaVersionTarget, String definition, String definitionName, Long parentId) {
		this.taskDefinitionRepository.save(new TaskDefinition(definitionName, definition));
		JobRepository jobRepository = jobRepositoryContainer.get(schemaVersionTarget.getName());
		TaskBatchDao taskBatchDao = taskBatchDaoContainer.get(schemaVersionTarget.getName());
		TaskExecutionDao taskExecutionDao = taskExecutionDaoContainer.get(schemaVersionTarget.getName());

		Pair<TaskExecution, JobExecution> jobExecutionPair = createSampleJob(jobRepository, taskBatchDao,
				taskExecutionDao, definitionName, BatchStatus.FAILED, insertTaskExecutionMetadata, schemaVersionTarget,
				parentId
		);
		return jobExecutionPair;
	}

	private Pair<TaskExecution, JobExecution> createSampleJob(
			JobRepository jobRepository,
			TaskBatchDao taskBatchDao,
			TaskExecutionDao taskExecutionDao,
			String jobName,
			BatchStatus status,
			boolean insertTaskExecutionMetadata,
			SchemaVersionTarget schemaVersionTarget, Long parentId
	) {
		JobInstance instance = jobRepository.createJobInstance(jobName, new JobParameters());
		jobInstanceCount++;
		TaskExecution taskExecution = parentId != null
				? taskExecutionDao.createTaskExecution(jobName, new Date(), Collections.singletonList("--spring.cloud.task.parent-schema-target=" + schemaVersionTarget.getName()), null, parentId)
				: taskExecutionDao.createTaskExecution(jobName, new Date(), Collections.singletonList("--spring.cloud.task.parent-schema-target=" + schemaVersionTarget.getName()), null);
		JobExecution jobExecution;
		JdbcTemplate template = new JdbcTemplate(this.dataSource);

		if (insertTaskExecutionMetadata) {
			template.execute(String.format("INSERT INTO %sEXECUTION_METADATA (ID, TASK_EXECUTION_ID, TASK_EXECUTION_MANIFEST) VALUES (%s, %s, '{\"taskDeploymentRequest\":{\"definition\":{\"name\":\"bd0917a\",\"properties\":{\"spring.datasource.username\":\"root\",\"spring.cloud.task.name\":\"bd0917a\",\"spring.datasource.url\":\"jdbc:mariadb://localhost:3306/task\",\"spring.datasource.driverClassName\":\"org.mariadb.jdbc.Driver\",\"spring.datasource.password\":\"password\"}},\"resource\":\"file:/Users/glennrenfro/tmp/batchdemo-0.0.1-SNAPSHOT.jar\",\"deploymentProperties\":{},\"commandlineArguments\":[\"run.id_long=1\",\"--spring.cloud.task.executionid=201\"]},\"platformName\":\"demo\"}')", schemaVersionTarget.getTaskPrefix(), taskExecution.getExecutionId(), taskExecution.getExecutionId()));
		}
		if(AppBootSchemaVersion.BOOT3.equals(schemaVersionTarget.getSchemaVersion())) {
			jobExecution = new JobExecution(instance, taskExecution.getExecutionId(), this.jobParameters, "foo");
			jobExecution.setCreateTime(new Date());
			jobExecution.setVersion(1);
			Object[] jobExecutionParameters = new Object[] {
				jobExecution.getId(),
				instance.getInstanceId(),
				new Date(),
				new Date(),
				BatchStatus.COMPLETED,
				ExitStatus.COMPLETED.getExitCode(),
				ExitStatus.COMPLETED.getExitDescription(),
				1,
				new Date(),
				new Date()
			};
			int[] argTypes = {Types.BIGINT, Types.BIGINT, Types.TIMESTAMP, Types.TIMESTAMP, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.TIMESTAMP, Types.TIMESTAMP};
			this.jdbcTemplate.update(SAVE_JOB_EXECUTION, jobExecutionParameters, argTypes);

			Object[] jobExecutionParmParameters = new Object[] { jobExecution.getId(),  "identifying.param", "java.lang.String", "testparm", "Y"};
			this.jdbcTemplate.update(SAVE_JOB_EXECUTION_PARAM, jobExecutionParmParameters, new int[] { Types.BIGINT,
					Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.CHAR});
		} else {
			jobExecution = jobRepository.createJobExecution(instance,
				this.jobParameters, null);
				StepExecution stepExecution = new StepExecution("foo", jobExecution, jobExecution.getJobId());
				stepExecution.setId(null);
				jobRepository.add(stepExecution);
		}
		taskBatchDao.saveRelationship(taskExecution, jobExecution);
		jobExecution.setStatus(status);
		jobExecution.setStartTime(new Date());
		ExitStatus exitStatus = new ExitStatus(status.getBatchStatus().name(), status.toString());
		jobExecution.setExitStatus(exitStatus);
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
		AppRegistration someName = new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld"));
		when(appRegistry.find(eq("some-name"), any(ApplicationType.class))).thenReturn(someName);
		AppRegistration someNameBoot3 = new AppRegistration("some-name-boot3", ApplicationType.task, "", URI.create("https://helloworld"), URI.create("https://helloworld"), AppBootSchemaVersion.fromBootVersion("3"));
		when(appRegistry.find(eq("some-name-boot3"), any(ApplicationType.class))).thenReturn(someNameBoot3);
		AppRegistration myJobOrig = new AppRegistration("myJob_ORIG", ApplicationType.task, URI.create("https://myjob"));
		AppRegistration myJobOrigBoot3 = new AppRegistration("myJob_ORIG", ApplicationType.task, "3.0.0", URI.create("https://myjob"), URI.create("https:/myjob/metadata"), AppBootSchemaVersion.fromBootVersion("3"));
		when(appRegistry.find(contains("myJob_ORIG"), any(ApplicationType.class), eq("3.0.0"))).thenReturn(myJobOrigBoot3);
		when(appRegistry.find(contains("myJob_ORIG"), any(ApplicationType.class))).thenReturn(myJobOrig);

		try {
			when(appRegistry.getAppResource(any())).thenReturn(new FileUrlResource("src/test/resources/apps/foo-task"));
		}
		catch (MalformedURLException e) {
			throw new IllegalStateException("Invalid File Resource Specified", e);
		}
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}
}
