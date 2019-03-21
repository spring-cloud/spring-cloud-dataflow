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

import org.junit.Test;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Janne Valkealahti
 * @author Mark Fisher
 */
public class DeploymentKeyTests {

	@Test
	public void testStreamAppDeploymentKeyEquals() {
		StreamDefinition streamDefinition1 = new StreamDefinition("myStream1", "time | log");
		StreamAppDefinition[] appDefinitions1 = streamDefinition1.getAppDefinitions()
				.toArray(new StreamAppDefinition[0]);
		StreamDefinition streamDefinition2 = new StreamDefinition("myStream1", "time | log");
		StreamAppDefinition[] appDefinitions2 = streamDefinition2.getAppDefinitions()
				.toArray(new StreamAppDefinition[0]);
		String appDeploymentKey1 = DeploymentKey.forStreamAppDefinition(appDefinitions1[0]);
		String appDeploymentKey2 = DeploymentKey.forStreamAppDefinition(appDefinitions1[1]);
		String appDeploymentKey3 = DeploymentKey.forStreamAppDefinition(appDefinitions2[0]);
		String appDeploymentKey4 = DeploymentKey.forStreamAppDefinition(appDefinitions2[1]);

		assertThat(streamDefinition1.equals(streamDefinition2), is(true));
		assertThat(appDeploymentKey1.equals(appDeploymentKey3), is(true));
		assertThat(appDeploymentKey2.equals(appDeploymentKey4), is(true));
	}

	@Test
	public void testTaskAppDeploymentKeyEquals() {
		TaskDefinition taskDefinition1 = new TaskDefinition("myTask1", "testTask");
		TaskDefinition taskDefinition2 = new TaskDefinition("myTask1", "testTask");
		String appDeploymentKey1 = DeploymentKey.forTaskDefinition(taskDefinition1);
		String appDeploymentKey2 = DeploymentKey.forTaskDefinition(taskDefinition1);
		String appDeploymentKey3 = DeploymentKey.forTaskDefinition(taskDefinition2);
		String appDeploymentKey4 = DeploymentKey.forTaskDefinition(taskDefinition2);

		assertThat(taskDefinition1.equals(taskDefinition2), is(true));
		assertThat(appDeploymentKey1.equals(appDeploymentKey3), is(true));
		assertThat(appDeploymentKey2.equals(appDeploymentKey4), is(true));
	}
}
