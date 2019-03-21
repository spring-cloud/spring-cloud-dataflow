/*
 * Copyright 2016-2017 the original author or authors.
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
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.EmbeddedDataSourceConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.dataflow.server.configuration.TaskDependencies;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { TaskDependencies.class, EmbeddedDataSourceConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class })
public class RdbmsTaskDefinitionRepositoryTests extends AbstractTaskDefinitionTests {

	@Autowired
	private DataSource dataSource;

	@Autowired
	private TaskDefinitionRepository rdbmsRepository;

	private JdbcTemplate template;

	@Before
	public void setup() throws Exception {
		template = new JdbcTemplate(dataSource);
		template.execute("DELETE FROM TASK_DEFINITIONS");
		repository = rdbmsRepository;
	}

	@Test
	public void findAllSortTestASC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC, "DEFINITION_NAME"),
				new Sort.Order(Sort.Direction.ASC, "DEFINITION"));
		String[] names = new String[] { "task1", "task2", "task3" };
		findAllSort(sort, names);
	}

	@Test
	public void findAllPageableTestASC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC, "DEFINITION_NAME"),
				new Sort.Order(Sort.Direction.ASC, "DEFINITION"));
		Pageable pageable = new PageRequest(0, 10, sort);
		String[] names = new String[] { "task1", "task2", "task3" };
		findAllPageable(pageable, names);
	}

	@Test
	public void findAllSortTestDESC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION_NAME"),
				new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		String[] names = new String[] { "task3", "task2", "task1" };
		findAllSort(sort, names);
	}

	@Test
	public void findAllPageableTestDESC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION_NAME"),
				new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		Pageable pageable = new PageRequest(0, 10, sort);

		String[] names = new String[] { "task3", "task2", "task1" };
		findAllPageable(pageable, names);
	}

	@Test
	public void findAllSortTestASCNameOnly() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC, "DEFINITION_NAME"));
		String[] names = new String[] { "task1", "task2", "task3" };
		findAllSort(sort, names);
	}

	@Test
	public void findAllPageableTestASCNameOnly() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC, "DEFINITION_NAME"));
		Pageable pageable = new PageRequest(0, 10, sort);

		String[] names = new String[] { "task1", "task2", "task3" };
		findAllPageable(pageable, names);
	}

	@Test
	public void findAllSortTestASCDefinitionOnly() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		String[] names = new String[] { "task1", "task2", "task3" };
		findAllSort(sort, names);
	}

	@Test
	public void findAllPageableTestDESCDefinitionOnly() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		Pageable pageable = new PageRequest(0, 10, sort);

		String[] names = new String[] { "task1", "task2", "task3" };
		findAllPageable(pageable, names);
	}

	@Test
	public void findAllPageablePage2TestASC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.ASC, "DEFINITION_NAME"),
				new Sort.Order(Sort.Direction.ASC, "DEFINITION"));
		Pageable pageable = new PageRequest(1, 2, sort);
		String[] names = new String[] { "task3" };
		findAllPageable(pageable, names);
	}

	@Test
	public void findAllPageablePage2TestDESC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION_NAME"),
				new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		Pageable pageable = new PageRequest(1, 2, sort);
		String[] names = new String[] { "task1" };
		findAllPageable(pageable, names);
	}

	@Test
	public void findAllPageableDefinitionStringTestDESC() {
		Sort sort = new Sort(new Sort.Order(Sort.Direction.DESC, "DEFINITION"));
		Pageable pageable = new PageRequest(1, 2, sort);
		String[] names = new String[] { "task3" };
		findAllPageable(pageable, names);
	}

	private void findAllPageable(Pageable pageable, String[] expectedOrder) {

		assertFalse(repository.findAll().iterator().hasNext());
		initializeRepositoryNotInOrder();

		final Iterable<TaskDefinition> items = repository.findAll(pageable);

		makeSortAssertions(items, expectedOrder);

	}

	private void findAllSort(Sort sort, String[] expectedOrder) {
		assertFalse(repository.findAll().iterator().hasNext());
		initializeRepositoryNotInOrder();

		final Iterable<TaskDefinition> items = repository.findAll(sort);

		makeSortAssertions(items, expectedOrder);
	}

	private void makeSortAssertions(Iterable<TaskDefinition> items, String[] expectedOrder) {
		int count = 0;
		List<TaskDefinition> definitions = new ArrayList<>();
		for (TaskDefinition item : items) {
			definitions.add(item);
			count++;
		}

		assertEquals(expectedOrder.length, count);
		int currentDefinitionOffset = 0;
		for (String name : expectedOrder) {
			assertEquals("definition name retrieved was not in the order expected", name,
					definitions.get(currentDefinitionOffset++).getName());
		}
	}
}
