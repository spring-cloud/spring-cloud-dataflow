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
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

/**
 * Provides the tests required for exercising a TaskDefinitionRepository impl.
 *
 * @author Michael Minella
 * @author Mark Fisher
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
public abstract class AbstractTaskDefinitionTests {
	protected TaskDefinitionRepository repository;

	@Test
	public void findOne() {
		TaskDefinition definition = new TaskDefinition("task1", "myTask");
		repository.save(definition);
		repository.save(new TaskDefinition("task2", "myTask"));
		repository.save(new TaskDefinition("task3", "myTask"));

		assertThat(repository.findById("task1")).hasValue(definition);
	}

	@Test
	public void findAllNone() {
		Pageable pageable = PageRequest.of(1, 10);

		Page<TaskDefinition> page = repository.findAll(pageable);

		assertThat(page.getTotalElements()).isEqualTo(0);
		assertThat(page.getNumber()).isEqualTo(1);
		assertThat(page.getNumberOfElements()).isEqualTo(0);
		assertThat(page.getSize()).isEqualTo(10);
		assertThat(page.getContent()).isEmpty();
	}

	@Test
	public void findAllPageable() {
		initializeRepository();
		Pageable pageable = PageRequest.of(0, 10);

		Page<TaskDefinition> page = repository.findAll(pageable);

		assertThat(page.getTotalElements()).isEqualTo(3);
		assertThat(page.getNumber()).isEqualTo(0);
		assertThat(page.getNumberOfElements()).isEqualTo(3);
		assertThat(page.getSize()).isEqualTo(10);
		assertThat(page.getContent()).hasSize(3);
	}

	@Test
	public void saveDuplicate() {
		assertThatExceptionOfType(DuplicateTaskException.class).isThrownBy(() -> {
			repository.save(new TaskDefinition("task1", "myTask"));
			repository.save(new TaskDefinition("task1", "myTask"));
		});
	}

	@Test
	public void saveAllDuplicate() {
		assertThatExceptionOfType(DuplicateTaskException.class).isThrownBy(() -> {
			List<TaskDefinition> definitions = new ArrayList<>();
			definitions.add(new TaskDefinition("task1", "myTask"));

			repository.save(new TaskDefinition("task1", "myTask"));
			repository.saveAll(definitions);
		});
	}

	@Test
	public void findOneNoneFound() {
		assertThat(repository.findById("notfound")).isEmpty();

		initializeRepository();

		assertThat(repository.findById("notfound")).isEmpty();
	}

	@Test
	public void exists() {
		assertThat(repository.existsById("exists")).isFalse();

		repository.save(new TaskDefinition("exists", "myExists"));

		assertThat(repository.existsById("exists")).isTrue();
		assertThat(repository.existsById("nothere")).isFalse();
	}

	@Test
	public void testFindAll() {
		assertThat(repository.findAll().iterator()).isExhausted();

		initializeRepository();

		Iterable<TaskDefinition> items = repository.findAll();

		int count = 0;
		for (@SuppressWarnings("unused")
		TaskDefinition item : items) {
			count++;
		}

		assertThat(count).isEqualTo(3);
	}

	@Test
	public void findAllSpecific() {
		assertThat(repository.findAll().iterator()).isExhausted();

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

		assertThat(count).isEqualTo(2);
	}

	@Test
	public void testCount() {
		assertThat(repository.count()).isEqualTo(0);

		initializeRepository();

		assertThat(repository.count()).isEqualTo(3);
	}

	@Test
	public void deleteNotFound() {
		repository.deleteById("notFound");
	}

	@Test
	public void delete() {
		initializeRepository();

		assertThat(repository.findById("task2")).isNotEmpty();

		repository.deleteById("task2");

		assertThat(repository.findById("task2")).isEmpty();
	}

	@Test
	public void deleteDefinition() {
		initializeRepository();

		assertThat(repository.findById("task2")).isNotEmpty();

		repository.delete(new TaskDefinition("task2", "myTask"));

		assertThat(repository.findById("task2")).isEmpty();
	}

	@Test
	public void deleteMultipleDefinitions() {
		initializeRepository();

		assertThat(repository.findById("task1")).isNotEmpty();
		assertThat(repository.findById("task2")).isNotEmpty();

		repository.deleteAll(Arrays.asList(new TaskDefinition("task1", "myTask"), new TaskDefinition("task2", "myTask")));

		assertThat(repository.findById("task1")).isEmpty();
		assertThat(repository.findById("task2")).isEmpty();
	}

	@Test
	public void deleteAllNone() {
		repository.deleteAll();
	}

	@Test
	public void testDeleteAll() {
		initializeRepository();

		assertThat(repository.count()).isEqualTo(3);
		repository.deleteAll();
		assertThat(repository.count()).isEqualTo(0);
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
