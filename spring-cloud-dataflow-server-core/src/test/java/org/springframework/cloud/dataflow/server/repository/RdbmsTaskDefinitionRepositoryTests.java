/*
 * Copyright 2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.server.configuration.TaskDependencies;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Glenn Renfro
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TaskDependencies.class,
		EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class})
public class RdbmsTaskDefinitionRepositoryTests extends AbstractTaskDefinitionTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private TaskDefinitionRepository rdbmsRepository;

	private JdbcTemplate template;

	@Before
	public void setup() throws Exception{
		template = new JdbcTemplate(dataSource);
		template.execute("DELETE FROM TASK_DEFINITIONS");
		repository = rdbmsRepository;
	}

	@Test
	public void findAllSortTestASC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC, "DEFINITION_NAME"), new Sort.Order(Sort.Direction.ASC, "DEFINITION"));
		String[] names = new String[]{"task1", "task2", "task3"};
		findAllSort(sort, names);
	}

	@Test
	public void findAllSortTestDESC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION_NAME"), new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		String[] names = new String[]{"task3", "task2", "task1"};
		findAllSort(sort, names);
	}

	@Test
	public void findAllSortTestASCNameOnly() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC, "DEFINITION_NAME"));
		String[] names = new String[]{"task1", "task2", "task3"};
		findAllSort(sort, names);
	}

	@Test
	public void findAllSortTestASCDefinitionOnly() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		String[] names = new String[]{"task1", "task2", "task3"};
		findAllSort(sort, names);
	}

	private void findAllSort(Sort sort, String[] expectedOrder) {
		assertFalse(repository.findAll().iterator().hasNext());

		initializeRepositoryNotInOrder();

		Iterable<TaskDefinition> items = repository.findAll(sort);

		int count = 0;
		List<TaskDefinition> definitions = new ArrayList<>();
		for (TaskDefinition item : items) {
			definitions.add(item);
			count++;
		}

		assertEquals(expectedOrder.length, count);
		int currentDefinitionOffset = 0;
		for (String name : expectedOrder) {
			assertEquals("definition name retrieve was not in the order expected", name,
					definitions.get(currentDefinitionOffset++).getName());
		}
	}
}
