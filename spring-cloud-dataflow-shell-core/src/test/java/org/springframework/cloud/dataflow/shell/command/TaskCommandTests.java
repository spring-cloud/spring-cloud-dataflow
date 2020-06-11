/*
 * Copyright 2015-2019 the original author or authors.
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

import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.table.Table;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Renfro
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 */
public class TaskCommandTests extends AbstractShellIntegrationTest {

	private static final String APPS_URI = "META-INF/test-task-apps.properties";

	private static final Logger logger = LoggerFactory.getLogger(TaskCommandTests.class);

	private static final String TASK_NAME = "foo" + UUID.randomUUID().toString();

	private static final String EXIT_MESSAGE = "exit";

	private static final String ERROR_MESSAGE = "error";

	private static final int EXIT_CODE = 20;

	private static final long TASK_EXECUTION_ID = 10000;

	private static final Date startTime = new Date();

	private static final Date endTime = new Date(startTime.getTime() + 5000);

	private static final String EXTERNAL_EXECUTION_ID = "WOW22";

	private static JdbcTemplate template;

	@BeforeClass
	public static void setUp() throws InterruptedException{
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

	@AfterClass
	public static void tearDown() {
		JdbcTemplate template = new JdbcTemplate(applicationContext.getBean(DataSource.class));
		template.afterPropertiesSet();
		final String TASK_EXECUTION_FORMAT = "DELETE FROM task_execution WHERE task_execution_id = %d";
		template.execute(String.format(TASK_EXECUTION_FORMAT, TASK_EXECUTION_ID - 1));
		template.execute(String.format(TASK_EXECUTION_FORMAT, TASK_EXECUTION_ID));
	}

	@Before
	public void registerApps() {
		AppRegistryService registry = applicationContext.getBean(AppRegistryService.class);
		registry.importAll(true, new ClassPathResource(APPS_URI));
	}

	@Test
	public void testTaskLaunch() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		task().launch(taskName);
	}

	@Test
	@Ignore
	public void testTaskLaunchCTRUsingAltCtrName() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "1: timestamp && 2: timestamp");
		//You can launch with an task, doesn't have to be a CTR.
		task().launchWithAlternateCTR(taskName, "timestamp");
	}

	@Test
	public void testGetLog() throws Exception{
		logger.info("Retrieving task execution log");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		task().getTaskExecutionLog(taskName);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetLogInvalidPlatform() throws Exception{
		logger.info("Retrieving task execution log");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		task().getTaskExecutionLogInvalidPlatform(taskName);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetLogInvalidId() throws Exception{
		task().getTaskExecutionLogInvalidId();
	}

	private void testInvalidCTRLaunch(String taskDefinition, String ctrAppName, String expectedExceptionMessage) {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, taskDefinition);
		boolean isExceptionThrown = false;
		try {
			task().launchWithAlternateCTR(taskName, ctrAppName);
		} catch (IllegalArgumentException e) {
			assertTrue(e.getMessage().contains(expectedExceptionMessage));
			isExceptionThrown =  true;
		}
		assertTrue("Expected IllegalArgumentException to have been thrown", isExceptionThrown);
	}

	@Test
	public void testExecutionStop() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		long id = task().launch(taskName);
		CommandResult cr = task().stop(id);
		assertTrue(cr.toString().contains(
				String.format("Request to stop the task execution with id(s): %s has been submitted", id)));
	}

	@Test
	public void testExecutionStopWithPlatform() {
		logger.info("Launching instance of task");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		long id = task().launch(taskName);
		CommandResult cr = task().stopForPlatform(id, "default");
		assertTrue(cr.toString().contains(
				String.format("Request to stop the task execution with id(s): %s for platform %s has been submitted", id, "default")));
	}

	@Test
	public void testExecutionStopInvalid() {
		boolean isException = false;
		try {
			task().stop(9001);
		} catch (IllegalArgumentException dfce) {
			assertTrue(dfce.getMessage().contains("Could not find TaskExecution with id 9001"));
			isException = true;
		}
		assertTrue("Expected IllegalArgumentException to have been thrown", isException);
	}

	@Test
	public void testCreateTask() {
		logger.info("Create Task Test");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
	}

	@Test
	public void destroySpecificTask() {
		logger.info("Create Task Test");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		logger.info("Destroy created task");
		task().destroyTask(taskName);
	}

	@Test
	public void destroySpecificTaskWithCleanup() {
		logger.info("Create Task Test");
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");
		CommandResult cr = task().taskExecutionList();
		assertTrue("task execution list by name command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		int rowCountBeforeLaunch = table.getModel().getRowCount();
		task().launch(taskName);
		cr = task().taskExecutionListByName(taskName);
		assertTrue("task execution list by name command must be successful", cr.isSuccess());
		table = (Table) cr.getResult();
		assertEquals("Number of rows returned was not expected", 2, table.getModel().getRowCount());
		logger.info("Destroy created task with the cleanup");
		task().destroyTask(taskName, true);
		cr = task().taskExecutionList();
		assertTrue("task execution list by name command must be successful", cr.isSuccess());
		table = (Table) cr.getResult();
		assertEquals("Number of rows returned was not expected", rowCountBeforeLaunch, table.getModel().getRowCount());
	}

	@Test
	public void destroyAllTasks() {
		logger.info("Create Task Test");
		String taskName1 = generateUniqueStreamOrTaskName();
		task().create(taskName1, "timestamp");
		String taskName2 = generateUniqueStreamOrTaskName();
		task().create(taskName2, "timestamp");
		task().destroyAllTasks();
	}

	@Test
	public void testTaskExecutionList() {
		logger.info("Retrieve Task Execution List Test");
		CommandResult cr = task().taskExecutionList();
		assertTrue("task execution list command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 5, table.getModel().getColumnCount());
		verifyTableValue(table, 0, 0, "Task Name");
		verifyTableValue(table, 0, 1, "ID");
		verifyTableValue(table, 0, 2, "Start Time");
		verifyTableValue(table, 0, 3, "End Time");
		verifyTableValue(table, 0, 4, "Exit Code");

		verifyTableValue(table, 1, 0, TASK_NAME);
		verifyTableValue(table, 1, 1, TASK_EXECUTION_ID);
		verifyTableValue(table, 1, 2, startTime);
		verifyTableValue(table, 1, 3, endTime);
		verifyTableValue(table, 1, 4, EXIT_CODE);
	}

	@Test
	public void testTaskExecutionListByName() {
		logger.info("Retrieve Task Execution List By Name Test");
		task().create("mytask", "timestamp");
		CommandResult cr = task().taskExecutionListByName("mytask");
		assertTrue("task execution list by name command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 5, table.getModel().getColumnCount());

		verifyTableValue(table,0, 0, "Task Name");
		verifyTableValue(table,0, 1, "ID");
		verifyTableValue(table,0, 2, "Start Time");
		verifyTableValue(table,0, 3, "End Time");
		verifyTableValue(table,0, 4, "Exit Code");
	}

	@Test
	public void testViewExecution() {
		logger.info("Retrieve Task Execution Status by Id");

		CommandResult idResult = task().taskExecutionList();
		Table result = (Table) idResult.getResult();
		long value = (long) result.getModel().getValue(findRowForExecutionId(result, TASK_EXECUTION_ID), 1);
		logger.info("Looking up id " + value);
		CommandResult cr = task().taskExecutionStatus(value);
		assertTrue("task execution status command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());
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
	public void testValidate() {
		String taskName = generateUniqueStreamOrTaskName();
		task().create(taskName, "timestamp");

		CommandResult cr = task().taskValidate(taskName);
		assertTrue("task validate status command must be successful", cr.isSuccess());
		List results = (List) cr.getResult();
		Table table = (Table) results.get(0);
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());

		verifyTableValue(table, 0, 0, "Task Name");
		verifyTableValue(table, 0, 1, "Task Definition");
		verifyTableValue(table, 1, 0, taskName);
		verifyTableValue(table, 1, 1, "timestamp");

		String message = String.format("\n%s is a valid task.", taskName);
		assertEquals(String.format("Notification should be: %s", message), message, results.get(1));

		table = (Table) results.get(2);
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());

		verifyTableValue(table, 0, 0, "App Name");
		verifyTableValue(table, 0, 1, "Validation Status");
		verifyTableValue(table, 1, 0, "task:" + taskName);
		verifyTableValue(table, 1, 1, "valid");
	}

	@Test
	public void testCurrentExecutions() {
		CommandResult cr = task().taskExecutionCurrent();
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 4, table.getModel().getColumnCount());
		verifyTableValue(table, 0, 0, "Platform Name");
		verifyTableValue(table, 0, 1, "Platform Type");
		verifyTableValue(table, 0, 2, "Execution Count");
		verifyTableValue(table, 0, 3, "Maximum Executions");

		verifyTableValue(table, 1, 0, "default");
		verifyTableValue(table, 1, 1, "Local");
		verifyTableValue(table, 1, 3, 20);
	}

	@Test
	public void testTaskExecutionCleanup() {
		CommandResult cr = task().taskExecutionCleanup(10000);
		assertThat(cr.getResult(), is("Request to clean up resources for task execution 10000 has been submitted"));
	}

	@Test
	public void testPlatformList() {
		CommandResult cr = task().taskPlatformList();
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 3, table.getModel().getColumnCount());
		assertEquals("First Row First Value should be: Platform Name", "Platform Name", table.getModel().getValue(0, 0));
		assertEquals("First Row Second Value should be: Platform Type", "Platform Type", table.getModel().getValue(0, 1));
		assertEquals("First Row Second Value should be: Description", "Description", table.getModel().getValue(0, 2));
		assertEquals("Second Row First Value should be: default", "default", table.getModel().getValue(1, 0));
		assertEquals("Second Row Second Value should be: Local", "Local", table.getModel().getValue(1, 1));
	}

	private void verifyTableValue(Table table, int row, int col, Object expected) {
		assertEquals(String.format("Row %d, Column %d should be: %s", row, col, expected),expected,
			table.getModel().getValue(row, col));
	}

	private int findRowForExecutionId(Table table, long id) {
		int result = -1;
		for(int rowNum = 0; rowNum < table.getModel().getRowCount(); rowNum++) {
			if(table.getModel().getValue(rowNum, 1).equals(id)) {
				result = rowNum;
				break;
			}
		}
		assertTrue("Task Execution Id specified was not found in execution list", id > -1);
		return result;
	}

}
