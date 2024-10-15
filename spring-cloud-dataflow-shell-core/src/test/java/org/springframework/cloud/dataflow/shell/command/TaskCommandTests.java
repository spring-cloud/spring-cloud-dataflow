/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.shell.command;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.rest.client.DataFlowClientException;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.dataflow.shell.command.support.TablesInfo;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.table.Table;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author Glenn Renfro
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Chris Bono
 * @author Corneil du Plessis
 */

class TaskCommandTests extends AbstractShellIntegrationTest {

	private static final String APPS_URI = "META-INF/test-task-apps.properties";

	private static final Logger logger = LoggerFactory.getLogger(TaskCommandTests.class);

	private static final String TASK_NAME = "foo" + UUID.randomUUID();

	private static final String EXIT_MESSAGE = "exit";

	private static final String ERROR_MESSAGE = "error";

	private static final int EXIT_CODE = 20;

	private static final long TASK_EXECUTION_ID = 10000;

	private static final LocalDateTime startTime = LocalDateTime.now();

	private static final LocalDateTime endTime = LocalDateTime.now().plusSeconds(5);

	private static final String EXTERNAL_EXECUTION_ID = "WOW22";

	private static final String BOOT3_SCHEMA = "boot2";

	private static JdbcTemplate template;

	@BeforeAll
	static void setUp() throws InterruptedException{
		Thread.sleep(2000);
		template = new JdbcTemplate(applicationContext.getBean(DataSource.class));
		template.afterPropertiesSet();

		template.update(
				"INSERT into TASK_EXECUTION(TASK_EXECUTION_ID, " + "START_TIME, " + "END_TIME, " + "TASK_NAME, "
						+ "EXIT_CODE, " + "EXIT_MESSAGE, " + "LAST_UPDATED," + "ERROR_MESSAGE, "
						+ "EXTERNAL_EXECUTION_ID)" + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
				TASK_EXECUTION_ID, startTime, endTime, TASK_NAME, EXIT_CODE, EXIT_MESSAGE, endTime, ERROR_MESSAGE,
				EXTERNAL_EXECUTION_ID);
		template.update(
				"INSERT into TASK_EXECUTION(TASK_EXECUTION_ID, " + "START_TIME, " + "END_TIME, " + "TASK_NAME, "
						+ "EXIT_CODE, " + "EXIT_MESSAGE, " + "LAST_UPDATED," + "ERROR_MESSAGE, "
						+ "EXTERNAL_EXECUTION_ID)" + "values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
				TASK_EXECUTION_ID - 1, startTime, null, TASK_NAME, null, EXIT_MESSAGE, null, ERROR_MESSAGE,
				EXTERNAL_EXECUTION_ID);

		template.update(
				"INSERT into task_deployment(id, object_version, task_deployment_id, task_definition_name, platform_name, created_on) " +
						"values (?,?,?,?,?,?)",
						1, 1, TASK_EXECUTION_ID, TASK_NAME, "default", startTime);

	}

	@AfterAll
	static void tearDown() {
		JdbcTemplate template = new JdbcTemplate(applicationContext.getBean(DataSource.class));
		template.afterPropertiesSet();
		final String TASK_EXECUTION_FORMAT = "DELETE FROM TASK_EXECUTION WHERE TASK_EXECUTION_ID = %d";
		template.execute(String.format(TASK_EXECUTION_FORMAT, TASK_EXECUTION_ID - 1));
		template.execute(String.format(TASK_EXECUTION_FORMAT, TASK_EXECUTION_ID));
	}

	@BeforeEach
	void registerApps() {
		AppRegistryService registry = applicationContext.getBean(AppRegistryService.class);
		registry.importAll(true, new ClassPathResource(APPS_URI));
	}

	@Test
	void taskLaunch() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		task().launch(taskName);
	}

	@Test
	@Disabled("Shell is merging 2 properties into a single property.")
	void taskLaunchCTRUsingAltCtrName() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "1: timestamp && 2: timestamp");
		//You can launch with an task, doesn't have to be a CTR.
		task().launchWithAlternateCTR(taskName, "timestamp");
	}

	@Test
	void getLog() throws Exception{
		logger.info("Retrieving task execution log");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		taskWithErrors().getTaskExecutionLog(taskName);
	}

	@Test
	void getLogInvalidPlatform() throws Exception{
		logger.info("Retrieving task execution log");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		assertThat(task().getTaskExecutionLogInvalidPlatform(taskName))
				.isEqualTo("Log could not be retrieved.  Verify that deployments are still available.");
	}

	@Test
	void getLogInvalidId() {
		assertThatThrownBy(() -> taskWithErrors().getTaskExecutionLogInvalidId())
				.isInstanceOf(RuntimeException.class)
				.hasCauseInstanceOf(DataFlowClientException.class)
				.hasMessageContaining("Could not find TaskExecution with id 88");
	}

	private void testInvalidCTRLaunch(String taskDefinition, String ctrAppName, String expectedExceptionMessage) {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, taskDefinition);
		assertThatThrownBy(()->{
			task().launchWithAlternateCTR(taskName, ctrAppName);
		}).isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining(expectedExceptionMessage);
	}

	@Test
	void executionStop() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		long id = task().launch(taskName);
		Object result = task().stop(id);
		assertThat(result.toString()).contains(
				String.format("Request to stop the task execution with id(s): %s has been submitted", id));
	}

	@Test
	void executionStopWithPlatform() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		long id = task().launch(taskName);
		Object result = task().stopForPlatform(id, "default");
		assertThat(result.toString()).contains(
				String.format("Request to stop the task execution with id(s): %s for platform %s has been submitted", id, "default"));
	}

	@Test
	void executionStopInvalid() {
		assertThatThrownBy(() -> taskWithErrors().stop(9001))
				.isInstanceOf(RuntimeException.class)
				.hasCauseInstanceOf(DataFlowClientException.class)
				.hasMessageContaining("Could not find TaskExecution with id 9001");
	}

	@Test
	void createTask() {
		logger.info("Create Task Test");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
	}

	@Test
	void destroySpecificTask() {
		logger.info("Create Task Test");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		logger.info("Destroy created task");
		task().destroyTask(taskName);
	}

	@Test
	void destroySpecificTaskWithCleanup() {
		logger.info("Create Task Test");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		Object result = task().taskExecutionList();
		Table table = (Table) result;
		int rowCountBeforeLaunch = table.getModel().getRowCount();
		task().launch(taskName);
		result = task().taskExecutionListByName(taskName);
		table = (Table) result;
		assertThat(table.getModel().getRowCount()).isEqualTo(2);
		logger.info("Destroy created task with the cleanup");
		task().destroyTask(taskName, true);
		result =task().taskExecutionList();
		table = (Table) result;
		assertThat(table.getModel().getRowCount()).isEqualTo(rowCountBeforeLaunch);
	}

	@Test
	void destroyAllTasks() {
		logger.info("Create Task Test");
		String taskName1 = generateUniqueStreamOrTaskName();
		task().create(taskName1, "timestamp");
		String taskName2 = generateUniqueStreamOrTaskName();
		task().create(taskName2, "timestamp");
		task().destroyAllTasks();
	}

	@Test
	void taskExecutionList() {
		logger.info("Retrieve Task Execution List Test");
		Object result = task().taskExecutionList();
		Table table = (Table) result;
		assertThat(table.getModel().getColumnCount()).isEqualTo(5);
		verifyTableValue(table, 0, 0, "Task Name");
		verifyTableValue(table, 0, 1, "ID");
		verifyTableValue(table, 0, 2, "Start Time");
		verifyTableValue(table, 0, 3, "End Time");
		verifyTableValue(table, 0, 4, "Exit Code");

		// other tests don't always clean up so need to check matching row
		int row = 0;
		for (int i = 0; i < table.getModel().getRowCount(); i++) {
			if (ObjectUtils.nullSafeEquals(table.getModel().getValue(i, 0), TASK_NAME)) {
				row = i;
				break;
			}
		}
		assertThat(row).isGreaterThan(0);

		verifyTableValue(table, row, 0, TASK_NAME);
		verifyTableValue(table, row, 1, TASK_EXECUTION_ID);
		verifyTableValue(table, row, 2, startTime);
		verifyTableValue(table, row, 3, endTime);
		verifyTableValue(table, row, 4, EXIT_CODE);
	}

	@Test
	void taskExecutionListByName() {
		logger.info("Retrieve Task Execution List By Name Test");
		task().create("mytask", "timestamp");
		Object result = task().taskExecutionListByName("mytask");
		Table table = (Table) result;
		assertThat(table.getModel().getColumnCount()).isEqualTo(5);

		verifyTableValue(table,0, 0, "Task Name");
		verifyTableValue(table,0, 1, "ID");
		verifyTableValue(table,0, 2, "Start Time");
		verifyTableValue(table,0, 3, "End Time");
		verifyTableValue(table,0, 4, "Exit Code");
	}

	@Test
	void viewExecution() {
		logger.info("Retrieve Task Execution Status by Id");

		Object idResult = task().taskExecutionList();
		Table idResultTable = (Table) idResult;
		long value = (long) idResultTable.getModel().getValue(findRowForExecutionId(idResultTable, TASK_EXECUTION_ID), 1);
		logger.info("Looking up id " + value);
		Object result = task().taskExecutionStatus(value);
		Table table = (Table) result;
		assertThat(table.getModel().getColumnCount()).isEqualTo(2);
		verifyTableValue(table, 0, 0, "Key ");
		verifyTableValue(table, 1, 0, "Id ");
		verifyTableValue(table, 2, 0, "Resource URL ");
		verifyTableValue(table, 3, 0, "Name ");
		verifyTableValue(table, 4, 0, "CLI Arguments ");
		verifyTableValue(table, 5, 0, "App Arguments ");
		verifyTableValue(table, 6, 0, "Deployment Properties ");
		verifyTableValue(table, 7, 0, "Job Execution Ids ");
		verifyTableValue(table, 8, 0, "Start Time ");
		verifyTableValue(table, 9, 0, "End Time ");
		verifyTableValue(table, 10, 0, "Exit Code ");
		verifyTableValue(table, 11, 0, "Exit Message ");
		verifyTableValue(table, 12, 0, "Error Message ");
		verifyTableValue(table, 13, 0, "External Execution Id ");

		verifyTableValue(table, 1, 1, TASK_EXECUTION_ID);
		verifyTableValue(table, 3, 1, TASK_NAME);
		verifyTableValue(table, 8, 1, startTime);
		verifyTableValue(table, 9, 1, endTime);
		verifyTableValue(table, 10, 1, EXIT_CODE);
		verifyTableValue(table, 11, 1, EXIT_MESSAGE);
		verifyTableValue(table, 12, 1, ERROR_MESSAGE);
		verifyTableValue(table, 13, 1, EXTERNAL_EXECUTION_ID);
	}

	@Test
	void validate() {
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");

		Object result = task().taskValidate(taskName);
		TablesInfo results = (TablesInfo) result;
		Table table = results.getTables().get(0);
		assertThat(table.getModel().getColumnCount()).isEqualTo(2);

		verifyTableValue(table, 0, 0, "Task Name");
		verifyTableValue(table, 0, 1, "Task Definition");
		verifyTableValue(table, 1, 0, taskName);
		verifyTableValue(table, 1, 1, "timestamp");

		String message = String.format("\n%s is a valid task.", taskName);
		assertThat(message).isEqualTo(results.getFooters().get(0));

		table = results.getTables().get(1);
		assertThat(table.getModel().getColumnCount()).isEqualTo(2);

		verifyTableValue(table, 0, 0, "App Name");
		verifyTableValue(table, 0, 1, "Validation Status");
		verifyTableValue(table, 1, 0, "task:" + taskName);
		verifyTableValue(table, 1, 1, "valid");
	}

	@Test
	void currentExecutions() {
		Object result = task().taskExecutionCurrent();
		Table table = (Table) result;
		assertThat(table.getModel().getColumnCount()).isEqualTo(4);
		verifyTableValue(table, 0, 0, "Platform Name");
		verifyTableValue(table, 0, 1, "Platform Type");
		verifyTableValue(table, 0, 2, "Execution Count");
		verifyTableValue(table, 0, 3, "Maximum Executions");

		verifyTableValue(table, 1, 0, "default");
		verifyTableValue(table, 1, 1, "Local");
		verifyTableValue(table, 1, 3, 20);
	}

	@Test
	void taskExecutionCleanupById() {
		Object result = task().taskExecutionCleanup(10000);
		assertThat(result).hasToString("Request to clean up resources for task execution 10000 has been submitted");
	}

	@Test
	void platformList() {
		Object result = task().taskPlatformList();
		Table table = (Table) result;
		assertThat(table.getModel().getColumnCount()).isEqualTo(3);
		assertThat(table.getModel().getValue(0, 0))
				.as("First Row First Value should be: Platform Name")
				.isEqualTo("Platform Name");
		assertThat(table.getModel().getValue(0, 1))
				.as("First Row Second Value should be: Platform Type")
				.isEqualTo("Platform Type");
		assertThat(table.getModel().getValue(0, 2))
				.as("First Row Third Value should be: Description")
				.isEqualTo("Description");
		assertThat(table.getModel().getValue(1, 0))
				.as("Second Row First Value should be: default")
				.isEqualTo("default");
		assertThat(table.getModel().getValue(1, 1))
				.as("Second Row Second Value should be: Local")
				.isEqualTo("Local");
	}

	private void verifyTableValue(Table table, int row, int col, Object expected) {
		assertThat(table.getModel().getValue(row, col))
				.as(String.format("Row %d, Column %d should be: %s", row, col, expected))
				.isEqualTo(expected);
	}

	private int findRowForExecutionId(Table table, long id) {
		int result = -1;
		for(int rowNum = 0; rowNum < table.getModel().getRowCount(); rowNum++) {
			if(table.getModel().getValue(rowNum, 1).equals(id)) {
				result = rowNum;
				break;
			}
		}
		assertThat(id).as("Task Execution Id specified was not found in execution list").isGreaterThan(-1);
		return result;
	}

}
