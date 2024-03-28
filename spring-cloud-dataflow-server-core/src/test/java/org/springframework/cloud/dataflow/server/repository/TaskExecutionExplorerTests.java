/*
 * Copyright 2016-2023 the original author or authors.
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

import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.server.task.DataflowTaskExplorer;
import org.springframework.cloud.dataflow.core.AppRegistration;
import org.springframework.cloud.dataflow.core.ApplicationType;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.registry.service.AppRegistryService;
import org.springframework.cloud.dataflow.server.configuration.TaskServiceDependencies;
import org.springframework.cloud.task.repository.TaskExecution;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = { TaskServiceDependencies.class }, properties = {
		"spring.main.allow-bean-definition-overriding=true" })
@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class TaskExecutionExplorerTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private DataflowTaskExplorer explorer;

	private JdbcTemplate template;

	@Autowired
	private AppRegistryService appRegistryService;

	@Autowired
	private TaskDefinitionRepository definitionRepository;

	@BeforeEach
	public void setup() throws Exception {
		template = new JdbcTemplate(dataSource);
		template.execute("DELETE FROM TASK_EXECUTION");
		TaskDefinition taskDefinition = new TaskDefinition("baz", "baz");
		definitionRepository.save(taskDefinition);
	}

	@Test
	public void testInitializer() {
		int actual = template.queryForObject(
			"SELECT COUNT(*) from TASK_EXECUTION", Integer.class);
		assertThat(actual).isEqualTo(0);
		actual = template.queryForObject(
			"SELECT COUNT(*) from TASK_EXECUTION_PARAMS", Integer.class);
		assertThat(actual).isEqualTo(0);
	}

	@Test
	public void testExplorerFindAll() {
		final int ENTRY_COUNT = 4;
		insertTestExecutionDataIntoRepo(template, 3L, "foo");
		insertTestExecutionDataIntoRepo(template, 2L, "foo");
		insertTestExecutionDataIntoRepo(template, 1L, "foo");
		insertTestExecutionDataIntoRepo(template, 0L, "foo");

		List<TaskExecution> resultList = explorer.findAll(PageRequest.of(0, 10)).getContent();
		assertThat(resultList.size()).isEqualTo(ENTRY_COUNT);
		Map<String, TaskExecution> actual = new HashMap<>();
		for (TaskExecution taskExecution : resultList) {
			String key = String.format("%d", taskExecution.getExecutionId());
			actual.put(key, taskExecution);
		}
		Set<String> allKeys = new HashSet<>();
		for (TaskExecution execution : actual.values()) {
			String key = String.format("%d", execution.getExecutionId());
			assertThat(allKeys.contains(key)).isFalse();
			allKeys.add(key);
		}
		assertThat(actual.size()).isEqualTo(allKeys.size());
	}

	@Test
	public void testExplorerFindByName() throws Exception {
		insertTestExecutionDataIntoRepo(template, 3L, "foo");
		insertTestExecutionDataIntoRepo(template, 2L, "bar");
		insertTestExecutionDataIntoRepo(template, 1L, "baz");
		insertTestExecutionDataIntoRepo(template, 0L, "fee");

		List<TaskExecution> resultList = explorer.findTaskExecutionsByName("fee", PageRequest.of(0, 10)).getContent();
		assertThat(resultList.size()).isEqualTo(1);
		TaskExecution taskExecution = resultList.get(0);
		assertThat(taskExecution.getExecutionId()).isEqualTo(0);
		assertThat(taskExecution.getTaskName()).isEqualTo("fee");
	}

	@Test
	public void testExplorerSort() throws Exception {
		when(appRegistryService.find(eq("baz"), any(ApplicationType.class))).thenReturn(new AppRegistration("baz", ApplicationType.task, "1.0.0", new URI("file://src/test/resources/register-all.txt"),null));
		insertTestExecutionDataIntoRepo(template, 3L, "foo");
		insertTestExecutionDataIntoRepo(template, 2L, "bar");
		insertTestExecutionDataIntoRepo(template, 1L, "baz");
		insertTestExecutionDataIntoRepo(template, 0L, "fee");

		List<TaskExecution> resultList = explorer.findAll(PageRequest.of(0, 10, Sort.by("TASK_EXECUTION_ID"))).getContent();
		assertThat(resultList.size()).isEqualTo(4);
		List<Long> ids = resultList.stream().map(TaskExecution::getExecutionId).collect(Collectors.toList());
		assertThat(ids).containsExactly(0L, 1L, 2L, 3L);
	}

	private void insertTestExecutionDataIntoRepo(JdbcTemplate template, long id, String taskName) {
		final String INSERT_STATEMENT = "INSERT INTO TASK_EXECUTION (task_execution_id, "
				+ "start_time, end_time, task_name, " + "exit_code,exit_message,last_updated) "
				+ "VALUES (?,?,?,?,?,?,?)";
		Object[] param = new Object[] { id, new Date(id), new Date(), taskName, 0, null, new Date() };
		template.update(INSERT_STATEMENT, param);
	}

}
