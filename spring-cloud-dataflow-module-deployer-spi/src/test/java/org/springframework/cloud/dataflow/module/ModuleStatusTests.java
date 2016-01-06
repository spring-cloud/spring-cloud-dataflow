/*
 * Copyright 2015 the original author or authors.
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

package org.springframework.cloud.dataflow.module;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.dataflow.module.DeploymentState.deployed;
import static org.springframework.cloud.dataflow.module.DeploymentState.failed;
import static org.springframework.cloud.dataflow.module.DeploymentState.partial;
import static org.springframework.cloud.dataflow.module.DeploymentState.undeployed;
import static org.springframework.cloud.dataflow.module.DeploymentState.unknown;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.Test;

import org.springframework.cloud.dataflow.core.ModuleDeploymentId;

/**
 * Tests for {@link ModuleStatus}.
 *
 * @author Patrick Peralta
 */
public class ModuleStatusTests {

	@Test
	public void testState() {
		assertThat(moduleStatus(deployed, failed).getState(), is(partial));
		assertThat(moduleStatus(failed, unknown).getState(), is(failed));
		assertThat(moduleStatus(deployed, undeployed).getState(), is(partial));
		assertThat(moduleStatus(deployed, unknown).getState(), is(partial));
		assertThat(moduleStatus(undeployed, unknown).getState(), is(partial));
		assertThat(moduleStatus(new DeploymentState[0]).getState(), is(unknown));
	}

	static ModuleStatus moduleStatus(DeploymentState... states) {
		ModuleStatus.Builder builder = ModuleStatus.of(new ModuleDeploymentId("group", "label"));
		for (DeploymentState state: states) {
			builder.with(new Status(state));
		}
		return builder.build();
	}


	static class Status implements ModuleInstanceStatus {

		private final String id = UUID.randomUUID().toString();

		private final DeploymentState state;

		public Status(DeploymentState state) {
			this.state = state;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public DeploymentState getState() {
			return state;
		}

		@Override
		public Map<String, String> getAttributes() {
			return Collections.emptyMap();
		}
	}

}
