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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.aggregate.task.TaskDefinitionReader;
import org.springframework.cloud.dataflow.schema.AggregateTaskExecution;
import org.springframework.cloud.dataflow.aggregate.task.AggregateExecutionSupport;
import org.springframework.cloud.dataflow.aggregate.task.AggregateTaskExplorer;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.schema.service.SchemaService;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.dataflow.server.repository.support.SchemaUtilities;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class TaskExecutionExplorerTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private AggregateTaskExplorer explorer;

	@Autowired
	private AggregateExecutionSupport aggregateExecutionSupport;

	private JdbcTemplate template;

	@Autowired
	private SchemaService schemaService;

	@Autowired
	private TaskDefinitionReader taskDefinitionReader;

	@Before
	public void setup() throws Exception {
		template = new JdbcTemplate(dataSource);
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			String prefix = target.getTaskPrefix();
			template.execute(SchemaUtilities.getQuery("DELETE FROM %PREFIX%EXECUTION", prefix));
		}
	}

	@Test
	public void testInitializer() throws Exception {
		for (SchemaVersionTarget target : schemaService.getTargets().getSchemas()) {
			String prefix = target.getTaskPrefix();
			int actual = template.queryForObject(SchemaUtilities.getQuery("SELECT COUNT(*) from %PREFIX%EXECUTION", prefix), Integer.class);
		assertEquals("expected 0 entries returned from task_execution", 0, actual);
			actual = template.queryForObject(SchemaUtilities.getQuery("SELECT COUNT(*) from %PREFIX%EXECUTION_PARAMS", prefix), Integer.class);
		assertEquals("expected 0 entries returned from task_execution_params", 0, actual);
	}
	}

	@Test
	public void testExplorerFindAll() throws Exception {
		final int ENTRY_COUNT = 4;
		insertTestExecutionDataIntoRepo(template, 3L, "foo");
		insertTestExecutionDataIntoRepo(template, 2L, "foo");
		insertTestExecutionDataIntoRepo(template, 1L, "foo");
		insertTestExecutionDataIntoRepo(template, 0L, "foo");

		List<AggregateTaskExecution> resultList = explorer.findAll(PageRequest.of(0, 10)).getContent();
		assertEquals(String.format("expected %s entries returned from task_execution", ENTRY_COUNT), ENTRY_COUNT,
				resultList.size());
		Map<String, AggregateTaskExecution> actual = new HashMap<>();
		for (AggregateTaskExecution taskExecution : resultList) {
			String key = String.format("%d:%s", taskExecution.getExecutionId(), taskExecution.getSchemaTarget());
			actual.put(key, taskExecution);
		}
		Set<String> allKeys = new HashSet<>();
		for (AggregateTaskExecution execution : actual.values()) {
			String key = String.format("%d:%s", execution.getExecutionId(), execution.getSchemaTarget());
			assertFalse("expected key id does not match actual", allKeys.contains(key));
			allKeys.add(key);
		}
		assertEquals(allKeys.size(), actual.size());
	}

	@Test
	public void testExplorerFindByName() throws Exception {
		insertTestExecutionDataIntoRepo(template, 3L, "foo");
		insertTestExecutionDataIntoRepo(template, 2L, "bar");
		insertTestExecutionDataIntoRepo(template, 1L, "baz");
		insertTestExecutionDataIntoRepo(template, 0L, "fee");

		List<AggregateTaskExecution> resultList = explorer.findTaskExecutionsByName("fee", PageRequest.of(0, 10)).getContent();
		assertEquals("expected 1 entries returned from task_execution", 1, resultList.size());
		AggregateTaskExecution taskExecution = resultList.get(0);
		assertEquals("expected execution id does not match actual", 0, taskExecution.getExecutionId());
		assertEquals("expected taskName does not match actual", "fee", taskExecution.getTaskName());
	}

	private void insertTestExecutionDataIntoRepo(JdbcTemplate template, long id, String taskName) {
		SchemaVersionTarget schemaVersionTarget = aggregateExecutionSupport.findSchemaVersionTarget(taskName, taskDefinitionReader);
		final String INSERT_STATEMENT = SchemaUtilities.getQuery("INSERT INTO %PREFIX%EXECUTION (task_execution_id, "
				+ "start_time, end_time, task_name, " + "exit_code,exit_message,last_updated) "
				+ "VALUES (?,?,?,?,?,?,?)", schemaVersionTarget.getTaskPrefix());
		Object[] param = new Object[] { id, new Date(id), new Date(), taskName, 0, null, new Date() };
		template.update(INSERT_STATEMENT, param);
	}

}
