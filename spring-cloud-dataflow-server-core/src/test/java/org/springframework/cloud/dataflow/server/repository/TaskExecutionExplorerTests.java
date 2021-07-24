/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Glenn Renfro
 */
@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class TaskExecutionExplorerTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private TaskExplorer explorer;

	private JdbcTemplate template;

	@BeforeEach
	public void setup() throws Exception {
		template = new JdbcTemplate(dataSource);
		template.execute("DELETE FROM task_execution");
	}

	@Test
	public void testInitializer() throws Exception {
		int actual = template.queryForObject("SELECT COUNT(*) from TASK_EXECUTION", Integer.class);
		assertEquals(0, actual, "expected 0 entries returned from task_execution");
		actual = template.queryForObject("SELECT COUNT(*) from TASK_EXECUTION_PARAMS", Integer.class);
		assertEquals(0, actual, "expected 0 entries returned from task_execution_params");
	}

	@Test
	public void testExplorerFindAll() throws Exception {
		final int ENTRY_COUNT = 4;
		insertTestExecutionDataIntoRepo(template, 3L, "foo");
		insertTestExecutionDataIntoRepo(template, 2L, "foo");
		insertTestExecutionDataIntoRepo(template, 1L, "foo");
		insertTestExecutionDataIntoRepo(template, 0L, "foo");

		List<TaskExecution> resultList = explorer.findAll(PageRequest.of(0, 10)).getContent();
		assertEquals(ENTRY_COUNT,
				resultList.size(),
				String.format("expected %s entries returned from task_execution", ENTRY_COUNT));
		Map<Long, TaskExecution> actual = new HashMap<>();
		for (int executionId = 0; executionId < ENTRY_COUNT; executionId++) {
			TaskExecution taskExecution = resultList.get(executionId);
			actual.put(taskExecution.getExecutionId(), taskExecution);
		}
		for (long executionId = 0; executionId < ENTRY_COUNT; executionId++) {
			TaskExecution taskExecution = actual.get(executionId);
			assertEquals(executionId, taskExecution.getExecutionId(), "expected execution id does not match actual");
			assertEquals("foo", taskExecution.getTaskName(), "expected taskName does not match actual");
		}

	}

	@Test
	public void testExplorerFindByName() throws Exception {
		insertTestExecutionDataIntoRepo(template, 3L, "foo");
		insertTestExecutionDataIntoRepo(template, 2L, "bar");
		insertTestExecutionDataIntoRepo(template, 1L, "baz");
		insertTestExecutionDataIntoRepo(template, 0L, "fee");

		List<TaskExecution> resultList = explorer.findTaskExecutionsByName("fee", PageRequest.of(0, 10)).getContent();
		assertEquals(1, resultList.size(), "expected 1 entries returned from task_execution");
		TaskExecution taskExecution = resultList.get(0);
		assertEquals(0, taskExecution.getExecutionId(), "expected execution id does not match actual");
		assertEquals("fee", taskExecution.getTaskName(), "expected taskName does not match actual");
	}

	private void insertTestExecutionDataIntoRepo(JdbcTemplate template, long id, String taskName) {
		final String INSERT_STATEMENT = "INSERT INTO task_execution (task_execution_id, "
				+ "start_time, end_time, task_name, " + "exit_code,exit_message,last_updated) "
				+ "VALUES (?,?,?,?,?,?,?)";
		Object[] param = new Object[] { id, new Date(id), new Date(), taskName, 0, null, new Date() };
		template.update(INSERT_STATEMENT, param);
	}

}
