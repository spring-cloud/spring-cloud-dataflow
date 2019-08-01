/*
 * Copyright 2019 the original author or authors.
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

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.Launcher;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.job.LauncherRepository;
import org.springframework.cloud.dataflow.server.repository.TaskDefinitionRepository;
import org.springframework.cloud.dataflow.server.service.TaskExecutionService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.cloud.dataflow.server.service.TaskSaveService;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TaskServiceDependencies.class}, properties = {
		"spring.main.allow-bean-definition-overriding=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
public class DefaultTaskJobServiceTests {

	private final static String BASE_TASK_NAME = "myTask";

	private final static String TASK_NAME_ORIG = BASE_TASK_NAME + "_ORIG";

	@Autowired
	private TaskDefinitionRepository taskDefinitionRepository;

	@Autowired
	TaskSaveService taskSaveService;

	@Autowired
	AppRegistryService appRegistry;

	@Autowired
	LauncherRepository launcherRepository;

	@Autowired
	TaskLauncher taskLauncher;

	@Autowired
	TaskExecutionService taskExecutionService;

	@Autowired
	TaskJobService taskJobService;

	@Autowired
	DataSource dataSource;

	JdbcOperations jdbcOperations;

	@Before
	public void setup() {
		this.launcherRepository.save(new Launcher("default", "local", this.taskLauncher));
		this.taskDefinitionRepository.save(new TaskDefinition(TASK_NAME_ORIG, "demo"));
		this.taskDefinitionRepository.findAll();
		this.jdbcOperations = new JdbcTemplate(this.dataSource);
		initializeSuccessfulRegistry(this.appRegistry);
		this.taskSaveService.saveTaskDefinition("simpleTask", "AAA --foo=bar");
		when(this.taskLauncher.launch(any())).thenReturn("0");
		this.jdbcOperations.execute("DELETE FROM TASK_TASK_BATCH");
		this.jdbcOperations.execute("DELETE FROM TASK_EXECUTION");
		setupDB();
	}


	@Test
	@DirtiesContext
	public void restartJobTestDuplicateTaskName() throws Exception{
		this.jdbcOperations.execute("INSERT INTO TASK_EXECUTION_PARAMS (TASK_EXECUTION_ID, TASK_PARAM) VALUES (1, '--spring.cloud.task.name=wombat')");
		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		this.taskJobService.restartJobExecution(0L);

		verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());
		AppDeploymentRequest request =  argumentCaptor.getAllValues().get(1);
		verifyBaseTaskDefinitionProperties(request);
	}

	@Test
	@DirtiesContext
	public void restartJobTestWithDataSourceInCommandLineArgs() throws Exception{
		this.jdbcOperations.execute("INSERT INTO TASK_EXECUTION_PARAMS (TASK_EXECUTION_ID, TASK_PARAM) VALUES (1, '--spring.cloud.task.name=wombat')");
		this.jdbcOperations.execute("INSERT INTO TASK_EXECUTION_PARAMS (TASK_EXECUTION_ID, TASK_PARAM) VALUES (1, '--spring.datasource.url=wombat')");
		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		taskJobService.restartJobExecution(0L);
		verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

		AppDeploymentRequest request =  argumentCaptor.getAllValues().get(1);
		//Verify that duplicate spring.cloud.task.name does not appear in both command line arg and definition.
		assertEquals(request.getCommandlineArguments().size(), 3);
		int dataSourcePropertyCount = 0;
		for(String commandLineArg : request.getCommandlineArguments()) {
			assertFalse(commandLineArg.startsWith("--spring.cloud.task.name="));
			if (commandLineArg.startsWith("--spring.datasource.url")) {
				dataSourcePropertyCount++;
			}
		}
		assertTrue(dataSourcePropertyCount == 1);
		assertTrue(request.getDefinition().getProperties().containsKey("spring.cloud.task.name"));
		assertFalse(request.getDefinition().getProperties().containsKey("spring.datasource.url"));
		assertEquals(request.getDefinition().getProperties().get("spring.cloud.task.name"), TASK_NAME_ORIG);
	}

	@Test
	@DirtiesContext
	public void restartJobTest() throws Exception{
		ArgumentCaptor<AppDeploymentRequest> argumentCaptor = ArgumentCaptor.forClass(AppDeploymentRequest.class);
		taskJobService.restartJobExecution(0L);
		verify(this.taskLauncher, atLeast(1)).launch(argumentCaptor.capture());

		AppDeploymentRequest request =  argumentCaptor.getAllValues().get(1);
		verifyBaseTaskDefinitionProperties(request);
	}

	private void setupDB() {
		this.taskExecutionService.executeTask(TASK_NAME_ORIG, new HashMap<>(), Collections.emptyList());
		this.jdbcOperations.execute("INSERT INTO BATCH_JOB_INSTANCE (JOB_INSTANCE_ID, JOB_NAME,JOB_KEY) " +
				"VALUES (0,'MYJOB','1234')");
		this.jdbcOperations.execute("INSERT INTO BATCH_JOB_EXECUTION (JOB_EXECUTION_ID, " +
				"JOB_INSTANCE_ID, CREATE_TIME, STATUS) VALUES (0,0, {ts '2019-08-01 11:36:08'}, 'FAILED')");
		this.jdbcOperations.execute("INSERT INTO TASK_TASK_BATCH (TASK_EXECUTION_ID, JOB_EXECUTION_ID) VALUES (1,0)");
		this.jdbcOperations.execute("INSERT INTO BATCH_JOB_EXECUTION_PARAMS (JOB_EXECUTION_ID, " +
				"TYPE_CD, KEY_NAME, STRING_VAL, IDENTIFYING) VALUES (0, 'STRING', '-spring.cloud.data.flow.platformname', 'default','N')");
		this.jdbcOperations.execute("INSERT INTO BATCH_JOB_EXECUTION_PARAMS (JOB_EXECUTION_ID, " +
				"TYPE_CD, KEY_NAME, STRING_VAL, IDENTIFYING) VALUES (0, 'STRING', '-spring.cloud.task.name', 'oldtaskname','N')");

	}

	private void verifyBaseTaskDefinitionProperties(AppDeploymentRequest request) {
		//Verify that duplicate spring.cloud.task.name does not appear in both command line arg and definition.
		assertEquals(request.getCommandlineArguments().size(), 2);
		for(String commandLineArg : request.getCommandlineArguments()) {
			assertFalse(commandLineArg.startsWith("--spring.cloud.task.name="));
		}
		assertTrue(request.getDefinition().getProperties().containsKey("spring.cloud.task.name"));
		assertTrue(request.getDefinition().getProperties().containsKey("spring.datasource.url"));
		assertTrue(request.getDefinition().getProperties().containsKey("spring.datasource.driverClassName"));
		assertTrue(request.getDefinition().getProperties().containsKey("spring.datasource.username"));
		assertEquals(request.getDefinition().getProperties().get("spring.cloud.task.name"), TASK_NAME_ORIG);
	}

	private static void initializeSuccessfulRegistry(AppRegistryService appRegistry) {
		when(appRegistry.find(anyString(), any(ApplicationType.class))).thenReturn(
				new AppRegistration("some-name", ApplicationType.task, URI.create("https://helloworld")));
		when(appRegistry.getAppResource(any())).thenReturn(new FileSystemResource("src/test/resources/apps/foo-task"));
		when(appRegistry.getAppMetadataResource(any())).thenReturn(null);
	}


}
