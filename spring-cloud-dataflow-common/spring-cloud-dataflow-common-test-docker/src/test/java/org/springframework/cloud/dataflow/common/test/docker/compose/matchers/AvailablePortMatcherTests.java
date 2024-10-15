/*
 * Copyright 2018-2019 the original author or authors.
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
package org.springframework.cloud.dataflow.common.test.docker.compose.matchers;

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.cloud.dataflow.common.test.docker.compose.connection.DockerPort;

import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.cloud.dataflow.common.test.docker.compose.matchers.AvailablePortMatcher.areAvailable;

public class AvailablePortMatcherTests {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void succeed_when_there_are_no_unavailable_ports() {
		List<DockerPort> unavailablePorts = emptyList();
		assertThat(unavailablePorts, areAvailable());
	}

	@Test
	public void throw_exception_when_there_are_some_unavailable_ports() {
		List<DockerPort> unavailablePorts = Arrays.asList(new DockerPort("0.0.0.0", 1234, 1234),
														 new DockerPort("1.2.3.4", 2345, 3456));
		exception.expect(AssertionError.class);
		exception.expectMessage("For host with ip address: 0.0.0.0");
		exception.expectMessage("external port '1234' mapped to internal port '1234' was unavailable");
		exception.expectMessage("For host with ip address: 1.2.3.4");
		exception.expectMessage("external port '2345' mapped to internal port '3456' was unavailable");
		assertThat(unavailablePorts, areAvailable());
	}

}
