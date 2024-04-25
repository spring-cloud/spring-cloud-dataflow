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

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provides the tests required for exercising a TaskDefinitionRepository impl.
 *
 * @author Michael Minella
 * @author Mark Fisher
 * @author Glenn Renfro
 */
public abstract class AbstractTaskDefinitionTests {
	protected TaskDefinitionRepository repository;

	@Test
	public void testFindOne() {
		TaskDefinition definition = new TaskDefinition("task1", "myTask");
		repository.save(definition);
		repository.save(new TaskDefinition("task2", "myTask"));
		repository.save(new TaskDefinition("task3", "myTask"));

		assertThat(repository.findById("task1")).hasValue(definition);
	}

	@Test
	public void testFindAllNone() {
		Pageable pageable = PageRequest.of(1, 10);

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
		Pageable pageable = PageRequest.of(0, 10);

		Page<TaskDefinition> page = repository.findAll(pageable);

		assertEquals(page.getTotalElements(), 3);
		assertEquals(page.getNumber(), 0);
		assertEquals(page.getNumberOfElements(), 3);
		assertEquals(page.getSize(), 10);
		assertEquals(page.getContent().size(), 3);
	}

	@Test
	public void testSaveDuplicate() {
		assertThrows(DuplicateTaskException.class, () -> {
			repository.save(new TaskDefinition("task1", "myTask"));
			repository.save(new TaskDefinition("task1", "myTask"));
		});
	}

	@Test
	public void testSaveAllDuplicate() {
		assertThrows(DuplicateTaskException.class, () -> {
			List<TaskDefinition> definitions = new ArrayList<>();
			definitions.add(new TaskDefinition("task1", "myTask"));

			repository.save(new TaskDefinition("task1", "myTask"));
			repository.saveAll(definitions);
		});
	}

	@Test
	public void testFindOneNoneFound() {
		assertThat(repository.findById("notfound")).isEmpty();

		initializeRepository();

		assertThat(repository.findById("notfound")).isEmpty();
	}

	@Test
	public void testExists() {
		assertFalse(repository.existsById("exists"));

		repository.save(new TaskDefinition("exists", "myExists"));

		assertTrue(repository.existsById("exists"));
		assertFalse(repository.existsById("nothere"));
	}

	@Test
	public void testFindAll() {
		assertFalse(repository.findAll().iterator().hasNext());

		initializeRepository();

		Iterable<TaskDefinition> items = repository.findAll();

		int count = 0;
		for (@SuppressWarnings("unused")
		TaskDefinition item : items) {
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

		Iterable<TaskDefinition> items = repository.findAllById(names);

		int count = 0;
		for (@SuppressWarnings("unused")
		TaskDefinition item : items) {
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
		repository.deleteById("notFound");
	}

	@Test
	public void testDelete() {
		initializeRepository();

		assertThat(repository.findById("task2")).isNotEmpty();

		repository.deleteById("task2");

		assertThat(repository.findById("task2")).isEmpty();
	}

	@Test
	public void testDeleteDefinition() {
		initializeRepository();

		assertThat(repository.findById("task2")).isNotEmpty();

		repository.delete(new TaskDefinition("task2", "myTask"));

		assertThat(repository.findById("task2")).isEmpty();
	}

	@Test
	public void testDeleteMultipleDefinitions() {
		initializeRepository();

		assertThat(repository.findById("task1")).isNotEmpty();
		assertThat(repository.findById("task2")).isNotEmpty();

		repository.deleteAll(Arrays.asList(new TaskDefinition("task1", "myTask"), new TaskDefinition("task2", "myTask")));

		assertThat(repository.findById("task1")).isEmpty();
		assertThat(repository.findById("task2")).isEmpty();
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
		repository.save(new TaskDefinition("task2", "myTaskB"));
		repository.save(new TaskDefinition("task3", "myTaskA"));
		repository.save(new TaskDefinition("task1", "myTaskC"));
	}
}
