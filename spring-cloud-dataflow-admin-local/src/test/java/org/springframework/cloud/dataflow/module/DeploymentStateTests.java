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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.dataflow.module.DeploymentState.deployed;
import static org.springframework.cloud.dataflow.module.DeploymentState.error;
import static org.springframework.cloud.dataflow.module.DeploymentState.failed;
import static org.springframework.cloud.dataflow.module.DeploymentState.incomplete;
import static org.springframework.cloud.dataflow.module.DeploymentState.reduce;
import static org.springframework.cloud.dataflow.module.DeploymentState.undeployed;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

/**
 * Tests for DeploymentState.
 *
 * @author Eric Bottard
 */
public class DeploymentStateTests {

	@Test
	public void testReduce() {
		for (DeploymentState value : DeploymentState.values()) {
			assertThat(reduce(setOf(value)), is(value));
		}

		assertThat(reduce(setOf(deployed, undeployed)), is(incomplete));
		assertThat(reduce(setOf(deployed, failed)), is(incomplete));
		assertThat(reduce(setOf(deployed, failed, error)), is(error));


	}

	private static Set<DeploymentState> setOf(DeploymentState... states) {
		Set<DeploymentState> result = new HashSet<>();
		for (DeploymentState state : states) {
			result.add(state);
		}
		return result;
	}

}