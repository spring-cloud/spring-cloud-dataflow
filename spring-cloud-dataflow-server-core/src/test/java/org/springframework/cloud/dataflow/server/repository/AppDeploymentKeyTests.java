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
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;

/**
 * @author Janne Valkealahti
 */
public class AppDeploymentKeyTests {

	@Test
	public void testAppDeploymentKeyEquals() {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream1", "time | log");
		ModuleDefinition[] moduleDefinitions1 = streamDefinition1.getModuleDefinitions().toArray(new ModuleDefinition[0]);
		StreamDefinition streamDefinition2 = new StreamDefinition("myStream1", "time | log");
		ModuleDefinition[] moduleDefinitions2 = streamDefinition1.getModuleDefinitions().toArray(new ModuleDefinition[0]);
		AppDeploymentKey appDeploymentKey1 = new AppDeploymentKey(streamDefinition1, moduleDefinitions1[0]);
		AppDeploymentKey appDeploymentKey2 = new AppDeploymentKey(streamDefinition1, moduleDefinitions1[1]);
		AppDeploymentKey appDeploymentKey3 = new AppDeploymentKey(streamDefinition2, moduleDefinitions2[0]);
		AppDeploymentKey appDeploymentKey4 = new AppDeploymentKey(streamDefinition2, moduleDefinitions2[1]);

		assertThat(streamDefinition1.equals(streamDefinition2), is(true));
		assertThat(appDeploymentKey1.equals(appDeploymentKey3), is(true));
		assertThat(appDeploymentKey2.equals(appDeploymentKey4), is(true));
	}

	@Test
	public void testMapOperations() {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream1", "time | log");
		ModuleDefinition[] moduleDefinitions1 = streamDefinition1.getModuleDefinitions().toArray(new ModuleDefinition[0]);
		StreamDefinition streamDefinition2 = new StreamDefinition("myStream1", "time | log");
		ModuleDefinition[] moduleDefinitions2 = streamDefinition1.getModuleDefinitions().toArray(new ModuleDefinition[0]);
		AppDeploymentKey appDeploymentKey1 = new AppDeploymentKey(streamDefinition1, moduleDefinitions1[0]);
		AppDeploymentKey appDeploymentKey2 = new AppDeploymentKey(streamDefinition1, moduleDefinitions1[1]);
		AppDeploymentKey appDeploymentKey3 = new AppDeploymentKey(streamDefinition2, moduleDefinitions2[0]);
		AppDeploymentKey appDeploymentKey4 = new AppDeploymentKey(streamDefinition2, moduleDefinitions2[1]);

		Map<AppDeploymentKey, String> map = new HashMap<>();
		map.put(appDeploymentKey1, "id1");
		String id1 = map.get(appDeploymentKey3);
		assertThat(id1, is("id1"));
		map.put(appDeploymentKey2, "id2");
		String id2 = map.get(appDeploymentKey4);
		assertThat(id2, is("id2"));
	}
}
