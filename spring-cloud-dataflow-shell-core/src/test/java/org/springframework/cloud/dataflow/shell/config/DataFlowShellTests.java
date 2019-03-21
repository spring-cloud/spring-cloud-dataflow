/*
 * Copyright 2017 the original author or authors.
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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.cloud.dataflow.rest.client.AggregateCounterOperations;
import org.springframework.cloud.dataflow.rest.client.DataFlowOperations;
import org.springframework.cloud.dataflow.shell.Target;
import org.springframework.cloud.dataflow.shell.TargetHolder;
import org.springframework.cloud.dataflow.shell.command.support.OpsType;
import org.springframework.cloud.dataflow.shell.command.support.RoleType;

/**
 * @author Gunnar Hillert
 */
public class DataFlowShellTests {

	@Test
	public void testHasAccessWithNoOperation() {
		final DataFlowShell dataFlowShell = new DataFlowShell();
		dataFlowShell.setDataFlowOperations(null);

		Assert.assertFalse(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.AGGREGATE_COUNTER));

	}

	@Test
	public void testHasAccessWithOperations() {
		final Target target = new Target("http://myUri");

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithAggregateCounterOperations(target);
		Assert.assertTrue(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.AGGREGATE_COUNTER));

	}

	@Test
	public void testHasAccessWithOperationsAndNullRole() {
		final Target target = new Target("http://myUri");

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithAggregateCounterOperations(target);
		Assert.assertTrue(dataFlowShell.hasAccess(null, OpsType.AGGREGATE_COUNTER));

	}

	@Test
	public void testHasAccessWithOperationsAndAuthenticationEnabledButNotAuthenticated() {
		final Target target = new Target("http://myUri");
		target.setAuthenticationEnabled(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithAggregateCounterOperations(target);
		Assert.assertFalse(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.AGGREGATE_COUNTER));

	}

	@Test
	public void testHasAccessWithOperationsAndAuthenticationEnabledAndAuthenticated() {
		final Target target = new Target("http://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.VIEW);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithAggregateCounterOperations(target);
		Assert.assertTrue(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.AGGREGATE_COUNTER));
	}

	@Test
	public void testHasWrongRoleWithOperationsAndAuthenticationEnabledAndAuthenticated() {

		final Target target = new Target("http://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.CREATE);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithAggregateCounterOperations(target);
		Assert.assertFalse(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.AGGREGATE_COUNTER));
	}

	@Test
	public void testHasNullRoleWithOperationsAndAuthenticationEnabledAndAuthenticated() {

		final Target target = new Target("http://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.CREATE);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithAggregateCounterOperations(target);
		Assert.assertTrue(dataFlowShell.hasAccess(null, OpsType.AGGREGATE_COUNTER));
	}

	@Test
	public void testHasAccessWithOperationsAndAuthenticationEnabledAndAuthenticatedAndAuthorizationDisabled() {
		final Target target = new Target("http://myUri", "username", "password", true);
		target.getTargetCredentials().getRoles().add(RoleType.CREATE);
		target.setAuthenticationEnabled(true);
		target.setAuthenticated(true);
		target.setAuthorizationEnabled(false);

		final DataFlowShell dataFlowShell = prepareDataFlowShellWithAggregateCounterOperations(target);
		Assert.assertTrue(dataFlowShell.hasAccess(RoleType.VIEW, OpsType.AGGREGATE_COUNTER));
	}

	private DataFlowShell prepareDataFlowShellWithAggregateCounterOperations(Target target) {
		final DataFlowShell dataFlowShell = new DataFlowShell();

		final DataFlowOperations dataFlowOperations = Mockito.mock(DataFlowOperations.class);
		final AggregateCounterOperations aggregateCounterOperations = Mockito.mock(AggregateCounterOperations.class);
		Mockito.when(dataFlowOperations.aggregateCounterOperations()).thenReturn(aggregateCounterOperations);
		dataFlowShell.setDataFlowOperations(dataFlowOperations);

		final TargetHolder targetHolder = new TargetHolder();
		targetHolder.setTarget(target);
		dataFlowShell.setTargetHolder(targetHolder);
		return dataFlowShell;
	}
}
