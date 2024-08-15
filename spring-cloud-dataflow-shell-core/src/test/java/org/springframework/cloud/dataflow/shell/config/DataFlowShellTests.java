/*
 * Copyright 2017-2018 the original author or authors.
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
package org.springframework.cloud.dataflow.shell.config;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.rest.client.StreamOperations;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
class DataFlowShellTests {

	@Test
	void hasAccessWithNoOperation() {
		final DataFlowShell dataFlowShell = new DataFlowShell();
		dataFlowShell.setDataFlowOperations(null);

		assertThat(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM)).isFalse();

	}

	@Test
	void hasAccessWithOperations() {
		final Target target = new Target("https://myUri");

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithStreamOperations(target);
		assertThat(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM)).isTrue();

	}

	@Test
	void hasAccessWithOperationsAndNullRole() {
		final Target target = new Target("https://myUri");

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithStreamOperations(target);
		assertThat(dataFlowShell.hasAccess(null, OpsType.STREAM)).isTrue();

	}

	@Test
	void hasAccessWithOperationsAndAuthenticationEnabledButNotAuthenticated() {
		final Target target = new Target("https://myUri");
		target.setAuthenticationEnabled(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithStreamOperations(target);
		assertThat(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM)).isFalse();

	}

	@Test
	void hasAccessWithOperationsAndAuthenticationEnabledAndAuthenticated() {
		final Target target = new Target("https://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.VIEW);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithStreamOperations(target);
		assertThat(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM)).isTrue();
	}

	@Test
	void hasNotAccessWithOperationsAndAuthenticationEnabledAndAuthenticated() {
		final Target target = new Target("https://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.CREATE);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);
		final DataFlowShell dataFlowShell = prepareDataFlowShellWithStreamOperations(target);
		assertThat(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM)).isFalse();
	}

	@Test
	void hasWrongRoleWithOperationsAndAuthenticationEnabledAndAuthenticated() {

		final Target target = new Target("https://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.CREATE);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithStreamOperations(target);
		assertThat(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.STREAM)).isFalse();
	}

	@Test
	void hasNullRoleWithOperationsAndAuthenticationEnabledAndAuthenticated() {

		final Target target = new Target("https://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.CREATE);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithStreamOperations(target);
		assertThat(dataFlowShell.hasAccess(null, OpsType.STREAM)).isTrue();
	}

	private DataFlowShell prepareDataFlowShellWithStreamOperations(Target target) {
		final DataFlowShell dataFlowShell = new DataFlowShell();

		final DataFlowOperations dataFlowOperations = Mockito.mock(DataFlowOperations.class);
		final StreamOperations streamOperations = Mockito.mock(StreamOperations.class);
		Mockito.when(dataFlowOperations.streamOperations()).thenReturn(streamOperations);
		dataFlowShell.setDataFlowOperations(dataFlowOperations);

		final TargetHolder targetHolder = new TargetHolder();
		targetHolder.setTarget(target);
		dataFlowShell.setTargetHolder(targetHolder);
		return dataFlowShell;
	}
}
