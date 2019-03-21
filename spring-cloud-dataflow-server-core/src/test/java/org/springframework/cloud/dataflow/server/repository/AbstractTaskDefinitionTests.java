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
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Provides the tests required for exercising a TaskDefinitionRepository impl.
 *
 * @author Michael Minella
 * @author Mark Fisher
 * @author Glenn Renfro
 */
public abstract class AbstractTaskDefinitionTests {
	@Test
	public void testFindOne() {
		TaskDefinition definition = new TaskDefinition("task1", "myTask");
		repository.save(definition);
		repository.save(new TaskDefinition("task2", "myTask"));
		repository.save(new TaskDefinition("task3", "myTask"));

		assertEquals(definition, repository.findOne("task1"));
	}

	protected TaskDefinitionRepository repository;

	@Test
	public void testFindAllNone() {
		Pageable pageable = new PageRequest(1, 10);

		Page<TaskDefinition> page = repository.findAll(pageable);

		assertEquals(page.getTotalElements(), 0);
		assertEquals(page.getNumber(), 1);
		assertEquals(page.getNumberOfElements(), 0);
		assertEquals(page.getSize(), 10);
		assertEquals(page.getContent().size(), 0);
	}

	@Test
	public void testFindAllPageable() {
		initializeRepository();
		Pageable pageable = new PageRequest(0, 10);

		Page<TaskDefinition> page = repository.findAll(pageable);

		assertEquals(page.getTotalElements(), 3);
		assertEquals(page.getNumber(), 0);
		assertEquals(page.getNumberOfElements(), 3);
		assertEquals(page.getSize(), 10);
		assertEquals(page.getContent().size(), 3);
	}

	@Test(expected = DuplicateTaskException.class)
	public void testSaveDuplicate() {
		repository.save(new TaskDefinition("task1", "myTask"));
		repository.save(new TaskDefinition("task1", "myTask"));
	}

	@Test(expected = DuplicateTaskException.class)
	public void testSaveAllDuplicate() {
		List<TaskDefinition> definitions = new ArrayList<>();
		definitions.add(new TaskDefinition("task1", "myTask"));

		repository.save(new TaskDefinition("task1", "myTask"));
		repository.save(definitions);
	}

	@Test
	public void testFindOneNoneFound() {
		assertNull(repository.findOne("notFound"));

		initializeRepository();

		assertNull(repository.findOne("notFound"));
	}

	@Test
	public void testExists() {
		assertFalse(repository.exists("exists"));

		repository.save(new TaskDefinition("exists", "myExists"));

		assertTrue(repository.exists("exists"));
		assertFalse(repository.exists("nothere"));
	}

	@Test
	public void testFindAll() {
		assertFalse(repository.findAll().iterator().hasNext());

		initializeRepository();

		Iterable<TaskDefinition> items = repository.findAll();

		int count = 0;
		for (@SuppressWarnings("unused") TaskDefinition item : items) {
			count++;
		}

		assertEquals(3, count);
	}

	@Test
	public void testFindAllSpecific() {
		assertFalse(repository.findAll().iterator().hasNext());

		initializeRepository();

		List<String> names = new ArrayList<>();
		names.add("task1");
		names.add("task2");

		Iterable<TaskDefinition> items = repository.findAll(names);

		int count = 0;
		for (@SuppressWarnings("unused") TaskDefinition item : items) {
			count++;
		}

		assertEquals(2, count);
	}

	@Test
	public void testCount() {
		assertEquals(0, repository.count());

		initializeRepository();

		assertEquals(3, repository.count());
	}

	@Test
	public void testDeleteNotFound() {
		repository.delete("notFound");
	}

	@Test
	public void testDelete() {
		initializeRepository();

		assertNotNull(repository.findOne("task2"));

		repository.delete("task2");

		assertNull(repository.findOne("task2"));
	}

	@Test
	public void testDeleteDefinition() {
		initializeRepository();

		assertNotNull(repository.findOne("task2"));

		repository.delete(new TaskDefinition("task2", "myTask"));

		assertNull(repository.findOne("task2"));
	}

	@Test
	public void testDeleteMultipleDefinitions() {
		initializeRepository();

		assertNotNull(repository.findOne("task1"));
		assertNotNull(repository.findOne("task2"));

		repository.delete(Arrays.asList(new TaskDefinition("task1", "myTask"), new TaskDefinition("task2", "myTask")));

		assertNull(repository.findOne("task1"));
		assertNull(repository.findOne("task2"));
	}

	@Test
	public void testDeleteAllNone() {
		repository.deleteAll();
	}

	@Test
	public void testDeleteAll() {
		initializeRepository();

		assertEquals(3, repository.count());
		repository.deleteAll();
		assertEquals(0, repository.count());
	}

	private void initializeRepository() {
		repository.save(new TaskDefinition("task1", "myTask"));
		repository.save(new TaskDefinition("task2", "myTask"));
		repository.save(new TaskDefinition("task3", "myTask"));
	}

	protected void initializeRepositoryNotInOrder() {
		repository.save(new TaskDefinition("task3", "myTaskA"));
		repository.save(new TaskDefinition("task2", "myTaskB"));
		repository.save(new TaskDefinition("task1", "myTaskC"));
	}

}
