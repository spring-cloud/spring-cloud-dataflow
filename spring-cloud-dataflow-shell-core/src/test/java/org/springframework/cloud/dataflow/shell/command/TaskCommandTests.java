/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
		String taskName = generateUniqueName();
		task().create(taskName, "timestamp");
		task().launch(taskName);
	}

	@Test
	public void testCreateTask() {
		logger.info("Create Task Test");
		String taskName = generateUniqueName();
		task().create(taskName, "timestamp");
	}

	@Test
	public void destroySpecificTask() {
		logger.info("Create Task Test");
		String taskName = generateUniqueName();
		task().create(taskName, "timestamp");
		logger.info("Destroy created task");
		task().destroyTask(taskName);
	}

	@Test
	public void destroyAllTasks() {
		logger.info("Create Task Test");
		String taskName1 = generateUniqueName();
		task().create(taskName1, "timestamp");
		String taskName2 = generateUniqueName();
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
		assertEquals("First column should be Task Name", "Task Name", table.getModel().getValue(0, 0));
		assertEquals("Second column should be ID", "ID", table.getModel().getValue(0, 1));
		assertEquals("Third column should be Start Time", "Start Time", table.getModel().getValue(0, 2));
		assertEquals("Fourth column should be End Time", "End Time", table.getModel().getValue(0, 3));
		assertEquals("Fifth column should be Exit Code", "Exit Code", table.getModel().getValue(0, 4));
		assertEquals("First column, second row should be " + TASK_NAME, TASK_NAME, table.getModel().getValue(1, 0));
		assertEquals("Second column, second row should be " + TASK_EXECUTION_ID, TASK_EXECUTION_ID,
				table.getModel().getValue(1, 1));
		assertEquals("Third column, second row should be " + startTime, startTime, table.getModel().getValue(1, 2));
		assertEquals("Fourth column, second row should be End Time" + endTime, endTime,
				table.getModel().getValue(1, 3));
		assertEquals("Fifth column, second row should be Exit Code" + EXIT_CODE, EXIT_CODE,
				table.getModel().getValue(1, 4));
	}

	@Test
	public void testTaskExecutionListByName() {
		logger.info("Retrieve Task Execution List By Name Test");
		task().create("mytask", "timestamp");
		CommandResult cr = task().taskExecutionListByName("mytask");
		assertTrue("task execution list by name command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 5, table.getModel().getColumnCount());
		assertEquals("First column should be Task Name", "Task Name", table.getModel().getValue(0, 0));
		assertEquals("Second column should be ID", "ID", table.getModel().getValue(0, 1));
		assertEquals("Third column should be Start Time", "Start Time", table.getModel().getValue(0, 2));
		assertEquals("Fourth column should be End Time", "End Time", table.getModel().getValue(0, 3));
		assertEquals("Fifth column should be Exit Code", "Exit Code", table.getModel().getValue(0, 4));
	}

	@Test
	public void testViewExecution() {
		logger.info("Retrieve Task Execution Status by Id");

		CommandResult idResult = task().taskExecutionList();
		Table result = (Table) idResult.getResult();

		long value = (long) result.getModel().getValue(1, 1);
		logger.info("Looking up id " + value);
		CommandResult cr = task().taskExecutionStatus(value);
		assertTrue("task execution status command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());
		assertEquals("First key should be Key", "Key ", table.getModel().getValue(0, 0));
		assertEquals("Second key should be Id ", "Id ", table.getModel().getValue(1, 0));
		assertEquals("Third key should be Name ", "Name ", table.getModel().getValue(2, 0));
		assertEquals("Fourth key should be Arguments", "Arguments ", table.getModel().getValue(3, 0));
		assertEquals("Fifth key should be Job Execution Ids  ", "Job Execution Ids ", table.getModel().getValue(4, 0));
		assertEquals("Sixth key should be Start Time  ", "Start Time ", table.getModel().getValue(5, 0));
		assertEquals("Seventh key should be End Time ", "End Time ", table.getModel().getValue(6, 0));
		assertEquals("Eighth key should be Exit Code ", "Exit Code ", table.getModel().getValue(7, 0));
		assertEquals("Nineth key should be Exit Message ", "Exit Message ", table.getModel().getValue(8, 0));
		assertEquals("Tenth key should be Error Message ", "Error Message ", table.getModel().getValue(9, 0));
		assertEquals("Eleventh key should be External Execution Id ", "External Execution Id ",
				table.getModel().getValue(10, 0));

		assertEquals("Second value should be " + TASK_EXECUTION_ID, TASK_EXECUTION_ID, table.getModel().getValue(1, 1));
		assertEquals("Third value should be " + TASK_NAME, TASK_NAME, table.getModel().getValue(2, 1));
		assertEquals("Sixth value should be " + startTime, startTime, table.getModel().getValue(5, 1));
		assertEquals("Seventh value should be " + endTime, endTime, table.getModel().getValue(6, 1));
		assertEquals("Eighth value should be " + EXIT_CODE, EXIT_CODE, table.getModel().getValue(7, 1));
		assertEquals("Nineth value should be " + EXIT_MESSAGE, EXIT_MESSAGE, table.getModel().getValue(8, 1));
		assertEquals("Tenth value should be  " + ERROR_MESSAGE, ERROR_MESSAGE, table.getModel().getValue(9, 1));
		assertEquals("Eleventh value should be  " + EXTERNAL_EXECUTION_ID, EXTERNAL_EXECUTION_ID,
				table.getModel().getValue(10, 1));
	}

	@Test
	public void testValidate() {
		String taskName = generateUniqueName();
		task().create(taskName, "timestamp");

		CommandResult cr = task().taskValidate(taskName);
		assertTrue("task validate status command must be successful", cr.isSuccess());
		List results = (List) cr.getResult();
		Table table = (Table) results.get(0);
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());
		assertEquals("First Row First Value should be: Task Name", "Task Name", table.getModel().getValue(0, 0));
		assertEquals("First Row Second Value should be: Task Definition", "Task Definition",
				table.getModel().getValue(0, 1));
		assertEquals("Second Row First Value should be: " + taskName, taskName, table.getModel().getValue(1, 0));
		assertEquals("Second Row Second Value should be: timestamp", "timestamp", table.getModel().getValue(1, 1));

		String message = String.format("\n%s is a valid task.", taskName);
		assertEquals(String.format("Notification should be: %s", message), message, results.get(1));

		table = (Table) results.get(2);
		assertEquals("Number of columns returned was not expected", 2, table.getModel().getColumnCount());
		assertEquals("First Row First Value should be: App Name", "App Name", table.getModel().getValue(0, 0));
		assertEquals("First Row Second Value should be: Validation Status", "Validation Status",
				table.getModel().getValue(0, 1));
		assertEquals("Second Row First Value should be: task:" + taskName, "task:" + taskName,
				table.getModel().getValue(1, 0));
		assertEquals("Second Row Second Value should be: valid", "valid", table.getModel().getValue(1, 1));
	}

	@Test
	public void testCurrentExecutions() {
		CommandResult idResult = task().taskExecutionCurrent();
		Table result = (Table) idResult.getResult();
		long value = (long) result.getModel().getValue(0, 1);
		assertEquals(1L, value);
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

}
