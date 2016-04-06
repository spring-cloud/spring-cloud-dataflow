/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.dataflow.server.repository;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.stream.test.junit.redis.RedisTestSupport;

/**
 * Tests for {@link RedisAppDeploymentRepository}.
 *
 * @author Janne Valkealahti
 */
public class RedisAppDeploymentRepositoryTests {

	@Rule
	public RedisTestSupport redisTestSupport = new RedisTestSupport();

	private RedisAppDeploymentRepository repository;

	@Before
	public void setUp() {
		repository = new RedisAppDeploymentRepository("RedisAppDeploymentRepositoryTests", redisTestSupport.getResource());
	}

	@Test
	public void testSimpleOperations() {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream1", "time | log");
		ModuleDefinition[] moduleDefinitions1 = streamDefinition1.getModuleDefinitions().toArray(new ModuleDefinition[0]);
		StreamDefinition streamDefinition2 = new StreamDefinition("myStream1", "time | log");
		ModuleDefinition[] moduleDefinitions2 = streamDefinition1.getModuleDefinitions().toArray(new ModuleDefinition[0]);
		TaskDefinition taskDefinition1 = new TaskDefinition("myTask", "timestamp");
		TaskDefinition taskDefinition2 = new TaskDefinition("myTask", "timestamp");
		AppDeploymentKey appDeploymentKey1 = new AppDeploymentKey(streamDefinition1, moduleDefinitions1[0]);
		AppDeploymentKey appDeploymentKey2 = new AppDeploymentKey(streamDefinition1, moduleDefinitions1[1]);
		AppDeploymentKey appDeploymentKey3 = new AppDeploymentKey(streamDefinition2, moduleDefinitions2[0]);
		AppDeploymentKey appDeploymentKey4 = new AppDeploymentKey(streamDefinition2, moduleDefinitions2[1]);
		AppDeploymentKey appDeploymentKey5 = new AppDeploymentKey(taskDefinition1, taskDefinition1.getModuleDefinition());
		AppDeploymentKey appDeploymentKey6 = new AppDeploymentKey(taskDefinition2, taskDefinition2.getModuleDefinition());

		AppDeploymentKey saved1 = repository.save(appDeploymentKey1, "id1");
		AppDeploymentKey saved2 = repository.save(appDeploymentKey2, "id2");
		AppDeploymentKey saved3 = repository.save(appDeploymentKey5, "id3");
		assertThat(appDeploymentKey1.equals(saved1), is(true));
		assertThat(appDeploymentKey2.equals(saved2), is(true));
		assertThat(appDeploymentKey5.equals(saved3), is(true));

		String findOne1 = repository.findOne(appDeploymentKey1);
		assertThat(findOne1, notNullValue());
		assertThat(findOne1, is("id1"));
		String findOne2 = repository.findOne(appDeploymentKey3);
		assertThat(findOne2, notNullValue());
		assertThat(findOne2, is("id1"));

		String findOne3 = repository.findOne(appDeploymentKey2);
		assertThat(findOne3, notNullValue());
		assertThat(findOne3, is("id2"));
		String findOne4 = repository.findOne(appDeploymentKey4);
		assertThat(findOne4, notNullValue());
		assertThat(findOne4, is("id2"));

		String findOne5 = repository.findOne(appDeploymentKey5);
		assertThat(findOne5, notNullValue());
		assertThat(findOne5, is("id3"));
		String findOne6 = repository.findOne(appDeploymentKey6);
		assertThat(findOne6, notNullValue());
		assertThat(findOne6, is("id3"));
	}
}
