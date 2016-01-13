/*
 * Copyright 2015 the original author or authors.
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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.shell.AbstractShellIntegrationTest;
import org.springframework.shell.core.CommandResult;
import org.springframework.shell.table.Table;

/**
 * @author Glenn Renfro
 */
public class TaskCommandTests extends AbstractShellIntegrationTest {

	private static final Logger logger = LoggerFactory.getLogger(TaskCommandTests.class);

	@Test
	public void testCreateTask() throws InterruptedException {
		logger.info("Create Task Test");
		String taskName = generateUniqueName();
		task().create(taskName, "foobar");
	}

	@Test
	public void testTaskExecutionList() throws InterruptedException {
		logger.info("Retrieve Task Execution List Test");
		CommandResult cr = task().taskExecutionList();
		assertTrue("task execution list command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 5, table.getModel().getColumnCount());
		assertEquals("First column should be Task Name", "Task Name", table.getModel().getValue(0,0));
		assertEquals("Second column should be ID", "ID", table.getModel().getValue(0,1));
		assertEquals("Third column should be Start Time", "Start Time", table.getModel().getValue(0,2));
		assertEquals("Fourth column should be End Time", "End Time", table.getModel().getValue(0,3));
		assertEquals("Fifth column should be Exit Code", "Exit Code", table.getModel().getValue(0,4));
	}

	@Test
	public void testTaskExecutionListByName() throws InterruptedException {
		logger.info("Retrieve Task Execution List By Name Test");
		CommandResult cr = task().taskExecutionListByName();
		assertTrue("task execution list by name command must be successful", cr.isSuccess());
		Table table = (Table) cr.getResult();
		assertEquals("Number of columns returned was not expected", 5, table.getModel().getColumnCount());
		assertEquals("First column should be Task Name", "Task Name", table.getModel().getValue(0,0));
		assertEquals("Second column should be ID", "ID", table.getModel().getValue(0,1));
		assertEquals("Third column should be Start Time", "Start Time", table.getModel().getValue(0,2));
		assertEquals("Fourth column should be End Time", "End Time", table.getModel().getValue(0,3));
		assertEquals("Fifth column should be Exit Code", "Exit Code", table.getModel().getValue(0,4));
	}
}
