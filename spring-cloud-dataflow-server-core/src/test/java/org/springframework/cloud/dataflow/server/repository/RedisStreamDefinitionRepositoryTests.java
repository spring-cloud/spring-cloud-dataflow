/*
 * Copyright 2015-2016 the original author or authors.
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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterableOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Random;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.stream.test.junit.redis.RedisTestSupport;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Tests for RedisStreamDefinitionRepository that connect to an actual redis instance.
 *
 * @author Eric Bottard
 * @author Janne Valkealahti
 */
public class RedisStreamDefinitionRepositoryTests {

	private RedisStreamDefinitionRepository repository;

	private String key;

	@Rule
	public RedisTestSupport redisTestSupport = new RedisTestSupport();

	@Before
	public void setUp() {
		key = "RedisStreamDefinitionRepositoryTests-" + new Random().nextLong();
		repository = new RedisStreamDefinitionRepository(key, redisTestSupport.getResource());
	}

	@Test
	public void testFindOne() {
		assertThat(repository.findOne("does-not-exist"), is(nullValue(StreamDefinition.class)));
		StreamDefinition entity = new StreamDefinition("does-exist", "time | log");
		repository.save(entity);
		StreamDefinition readBack = repository.findOne("does-exist");
		assertThat(readBack.getName(), is("does-exist"));
		assertThat(readBack.getDslText(), is("time | log"));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFindAll() {
		assertThat(repository.findAll(), is(emptyIterableOf(StreamDefinition.class)));
		repository.save(new StreamDefinition("b", "time | log"));
		repository.save(new StreamDefinition("a", "time | log"));
		Iterable<StreamDefinition> result = repository.findAll();
		assertThat(result, contains(hasProperty("name", is("a")), hasProperty("name", is("b"))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFindAllWithSort() {
		Sort sort = new Sort(Sort.Direction.DESC, "name");
		assertThat(repository.findAll(sort), is(emptyIterableOf(StreamDefinition.class)));
		repository.save(new StreamDefinition("b", "time | log"));
		repository.save(new StreamDefinition("a", "time | log"));
		Iterable<StreamDefinition> result = repository.findAll(sort);
		assertThat(result, contains(hasProperty("name", is("b")), hasProperty("name", is("a"))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFindAllWithPageable() {
		PageRequest request = new PageRequest(1, 3, Sort.Direction.DESC, "name");
		assertThat(repository.findAll(request), is(emptyIterableOf(StreamDefinition.class)));
		for (int i = 0; i < 10; i++) {
			repository.save(new StreamDefinition("a" + i, "time | log"));
		}
		Iterable<StreamDefinition> result = repository.findAll(request);
		assertThat(result, contains(
				hasProperty("name", is("a6")),
				hasProperty("name", is("a5")),
				hasProperty("name", is("a4"))));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSaveIterable() {
		repository.save(Arrays.asList(new StreamDefinition("a", "time | log"), new StreamDefinition("b", "time | log")));
		Iterable<StreamDefinition> result = repository.findAll();
		assertThat(result, contains(hasProperty("name", is("a")), hasProperty("name", is("b"))));
	}

	@Test
	public void testExists() {
		assertThat(repository.exists("does-not-exist"), is(false));
		StreamDefinition entity = new StreamDefinition("does-exist", "time | log");
		repository.save(entity);
		assertThat(repository.exists("does-exist"), is(true));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFindAllByIds() {
		repository.save(new StreamDefinition("c", "time | log"));
		repository.save(new StreamDefinition("a", "time | log"));
		assertThat(repository.findAll(Arrays.asList("a", "b", "c")), contains(
				hasProperty("name", is("a")),
				nullValue(StreamDefinition.class),
				hasProperty("name", is("c"))));
	}

	@Test
	public void testCount() {
		repository.save(new StreamDefinition("c", "time | log"));
		repository.save(new StreamDefinition("a", "time | log"));
		assertThat(repository.count(), is(2L));
	}

	@Test
	public void testDelete() {
		repository.delete("does-not-exist");
		repository.delete(new StreamDefinition("does-not-exist", "time | log"));
		repository.delete(Arrays.asList(new StreamDefinition("does-not-exist", "time | log"),
				new StreamDefinition("does-not-exist-either", "time | log")));

		for (int i = 0; i < 10; i++) {
			repository.save(new StreamDefinition("a" + i, "time | log"));
		}

		repository.delete("a1");
		assertThat(repository.exists("a1"), is(false));

		repository.delete(new StreamDefinition("a3", "time | log"));
		assertThat(repository.exists("a3"), is(false));

		repository.delete(Arrays.asList(new StreamDefinition("a3", "time | log"), // already gone
				new StreamDefinition("a4", "time | log"),
				new StreamDefinition("a5", "time | log")));
		assertThat(repository.exists("a4"), is(false));
		assertThat(repository.exists("a5"), is(false));

	}

	@Test
	public void testDeleteAll() {
		repository.save(new StreamDefinition("b", "time | log"));
		repository.save(new StreamDefinition("a", "time | log"));
		repository.deleteAll();
		assertThat(repository.count(), is(0L));
	}

	@Test(expected = DuplicateStreamDefinitionException.class)
	public void testUpdate() {
		//TODO: This test will need to change once GH-331 is done
		repository.save(new StreamDefinition("a", "time | log"));
		assertThat(repository.findOne("a"), hasProperty("dslText", is("time | log")));
		repository.save(new StreamDefinition("a", "time | file"));
	}
}
