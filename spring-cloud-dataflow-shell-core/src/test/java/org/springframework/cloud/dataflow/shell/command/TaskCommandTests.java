/*
 * Copyright 2015-2017 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.cloud.deployer.resource.registry.UriRegistry;
import org.springframework.cloud.deployer.resource.registry.UriRegistryPopulator;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.table.Table;

/**
 * @author Glenn Renfro
 */
public class TaskCommandTests extends AbstractShellIntegrationTest {

	private static final String APPS_URI = "classpath:META-INF/test-task-apps.properties";

	private static final Logger logger = LoggerFactory.getLogger(TaskCommandTests.class);

	private static JdbcTemplate template;

	private static final String TASK_NAME = "foo" + UUID.randomUUID().toString();

	private static final String EXIT_MESSAGE = "exit";

	private static final String ERROR_MESSAGE = "error";

	private static final int EXIT_CODE = 20;

	private static final long TASK_EXECUTION_ID = 10000;

	private static final Date startTime = new Date();

	private static final Date endTime = new Date(startTime.getTime() + 5000);

	private static final String EXTERNAL_EXECUTION_ID = "WOW22";

	@Before
	public void registerApps() {
		UriRegistry registry = applicationContext.getBean(UriRegistry.class);
		UriRegistryPopulator populator = new UriRegistryPopulator();
		populator.setResourceLoader(new DefaultResourceLoader());
		populator.populateRegistry(true, registry, APPS_URI);
	}

	@BeforeClass
	public static void setUp() {
		template = new JdbcTemplate(applicationContext.getBean(DataSource.class));
		template.afterPropertiesSet();

		template.update("INSERT into TASK_EXECUTION(TASK_EXECUTION_ID, " +
				"START_TIME, " +
				"END_TIME, " +
				"TASK_NAME, " +
				"EXIT_CODE, " +
				"EXIT_MESSAGE, " +
				"LAST_UPDATED," +
				"ERROR_MESSAGE, " +
				"EXTERNAL_EXECUTION_ID)" +
				"values (?, ?, ?, ?, ?, ?, ?, ?, ?)",
				TASK_EXECUTION_ID,
				startTime,
				endTime,
				TASK_NAME,
				EXIT_CODE,
				EXIT_MESSAGE,
				endTime,
				ERROR_MESSAGE,
				EXTERNAL_EXECUTION_ID);
	}

	@Test
	public void testCreateTask() throws InterruptedException {
		logger.info("Create Task Test");
		String taskName = generateUniqueName();
		task().create(taskName, "timestamp");
	}

	@Test
	public void testTaskExecutionList() throws InterruptedException {
		logger.info("Retrieve Task Execution List Test");
		CommandResult cr = task().taskExecutionList();
		assertTrue("task execution list command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 5,
				table.getModel().getColumnCount());
		assertEquals("First column should be Task Name", "Task Name",
				table.getModel().getValue(0,0));
		assertEquals("Second column should be ID", "ID",
				table.getModel().getValue(0,1));
		assertEquals("Third column should be Start Time", "Start Time",
				table.getModel().getValue(0,2));
		assertEquals("Fourth column should be End Time", "End Time",
				table.getModel().getValue(0,3));
		assertEquals("Fifth column should be Exit Code", "Exit Code",
				table.getModel().getValue(0,4));
		assertEquals("First column, second row should be " + TASK_NAME,
				TASK_NAME, table.getModel().getValue(1,0));
		assertEquals("Second column, second row should be " + TASK_EXECUTION_ID,
				TASK_EXECUTION_ID, table.getModel().getValue(1,1));
		assertEquals("Third column, second row should be " + startTime,
				startTime, table.getModel().getValue(1,2));
		assertEquals("Fourth column, second row should be End Time" + endTime,
				endTime, table.getModel().getValue(1,3));
		assertEquals("Fifth column, second row should be Exit Code" + EXIT_CODE,
				EXIT_CODE, table.getModel().getValue(1,4));
	}

	@Test
	public void testTaskExecutionListByName() throws InterruptedException {
		logger.info("Retrieve Task Execution List By Name Test");
		task().create("mytask", "timestamp");
		CommandResult cr = task().taskExecutionListByName("mytask");
		assertTrue("task execution list by name command must be successful",
				cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 5,
				table.getModel().getColumnCount());
		assertEquals("First column should be Task Name", "Task Name",
				table.getModel().getValue(0,0));
		assertEquals("Second column should be ID", "ID",
				table.getModel().getValue(0,1));
		assertEquals("Third column should be Start Time", "Start Time",
				table.getModel().getValue(0,2));
		assertEquals("Fourth column should be End Time", "End Time",
				table.getModel().getValue(0,3));
		assertEquals("Fifth column should be Exit Code", "Exit Code",
				table.getModel().getValue(0,4));
	}

	@Test
	public void testViewExecution() throws InterruptedException {
		logger.info("Retrieve Task Execution Status by Id");

		CommandResult idResult = task().taskExecutionList();
		Table result = (Table) idResult.getResult();

		long value = (long) result.getModel().getValue(1, 1);
		logger.info("Looking up id " + value);
		CommandResult cr = task().taskExecutionStatus(value);
		assertTrue("task execution status command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 2,
				table.getModel().getColumnCount());
		assertEquals("First key should be Key", "Key ",
				table.getModel().getValue(0, 0));
		assertEquals("Second key should be Id ", "Id ",
				table.getModel().getValue(1, 0));
		assertEquals("Third key should be Name ", "Name ",
				table.getModel().getValue(2, 0));
		assertEquals("Fourth key should be Arguments", "Arguments ",
				table.getModel().getValue(3, 0));
		assertEquals("Fifth key should be Job Execution Ids  ",
				"Job Execution Ids ", table.getModel().getValue(4, 0));
		assertEquals("Sixth key should be Start Time  ", "Start Time ",
				table.getModel().getValue(5, 0));
		assertEquals("Seventh key should be End Time ", "End Time ",
				table.getModel().getValue(6, 0));
		assertEquals("Eighth key should be Exit Code ", "Exit Code ",
				table.getModel().getValue(7, 0));
		assertEquals("Nineth key should be Exit Message ", "Exit Message ",
				table.getModel().getValue(8, 0));
		assertEquals("Tenth key should be Error Message ", "Error Message ",
				table.getModel().getValue(9, 0));
		assertEquals("Eleventh key should be External Execution Id ", "External Execution Id ",
				table.getModel().getValue(10, 0));

		assertEquals("Second value should be " + TASK_EXECUTION_ID,
				TASK_EXECUTION_ID, table.getModel().getValue(1, 1));
		assertEquals("Third value should be " + TASK_NAME, TASK_NAME,
				table.getModel().getValue(2, 1));
		assertEquals("Sixth value should be " + startTime, startTime,
				table.getModel().getValue(5, 1));
		assertEquals("Seventh value should be " + endTime, endTime,
				table.getModel().getValue(6, 1));
		assertEquals("Eighth value should be " + EXIT_CODE, EXIT_CODE,
				table.getModel().getValue(7, 1));
		assertEquals("Nineth value should be " + EXIT_MESSAGE, EXIT_MESSAGE,
				table.getModel().getValue(8, 1));
		assertEquals("Tenth value should be  " + ERROR_MESSAGE, ERROR_MESSAGE,
				table.getModel().getValue(9, 1));
		assertEquals("Eleventh value should be  " + EXTERNAL_EXECUTION_ID,
				EXTERNAL_EXECUTION_ID,
				table.getModel().getValue(10, 1));

	}
}
