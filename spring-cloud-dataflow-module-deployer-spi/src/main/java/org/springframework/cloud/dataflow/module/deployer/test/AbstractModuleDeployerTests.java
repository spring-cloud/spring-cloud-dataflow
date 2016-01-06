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

package org.springframework.cloud.dataflow.module.deployer.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.dataflow.module.DeploymentState.deployed;
import static org.springframework.cloud.dataflow.module.DeploymentState.unknown;
import static org.springframework.cloud.dataflow.module.deployer.test.EventuallyMatcher.eventually;

import java.util.UUID;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsMapContaining;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.core.ArtifactCoordinates;
import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.deployer.ModuleDeployer;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Abstract base class for integration tests of
 * {@link org.springframework.cloud.dataflow.module.deployer.ModuleDeployer} implementations.
 *
 * <p>Inheritors should setup an environment with a newly created {@link ModuleDeployer} that has no pre-deployed
 * modules. Tests in this class are independent and leave the deployer in a clean state after they successfully run.</p>
 *
 * <p>As deploying a module is often quite time consuming, some tests often test various aspects of deployment in a
 * row, to avoid re-deploying modules over and over again.</p>
 *
 * @author Eric Bottard
 */
@RunWith(SpringJUnit4ClassRunner.class)
public abstract class AbstractModuleDeployerTests {

	@Autowired
	protected ModuleDeployer moduleDeployer;

	@Test
	public void newlyCreatedDeployerShouldReportNoModules() {
		assertThat(moduleDeployer.status().isEmpty(), is(true));
	}

	@Test
	public void testUnknownDeployment() {
		ModuleDeploymentId id = new ModuleDeploymentId("agroup", "alabel");
		ModuleStatus status = moduleDeployer.status(id);

		assertThat(status.getModuleDeploymentId(), is(id));
		assertThat("The map was not empty: " + status.getInstances(), status.getInstances().isEmpty(), is(true));
		assertThat(status.getState(), is(unknown));
	}

	@Test
	public void testSimpleDeployment() {
		ModuleDefinition definition = new ModuleDefinition.Builder()
				.setGroup(randomName())
				.setName(randomName())
				.build();
		ArtifactCoordinates coordinates = ArtifactCoordinates
				.parse("org.springframework.cloud.stream.module:time-source:jar:exec:1.0.0.M2");
		ModuleDeploymentRequest request = new ModuleDeploymentRequest(definition, coordinates);

		ModuleDeploymentId deploymentId = moduleDeployer.deploy(request);
		Attempts timeout = deploymentTimeout();
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.<ModuleStatus>hasProperty("state", is(deployed))), timeout.noAttempts, timeout.pause));

		// Use this opportunity to also test the multi-status query
		assertThat(moduleDeployer.status(), IsMapContaining.hasEntry(is(deploymentId),
				Matchers.<ModuleStatus>hasProperty("state", is(deployed))));

		timeout = undeploymentTimeout();
		moduleDeployer.undeploy(deploymentId);
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.<ModuleStatus>hasProperty("state", is(unknown))), timeout.noAttempts, timeout.pause));
		assertThat(moduleDeployer.status().isEmpty(), is(true));
	}

	protected String randomName() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Return the timeout to use for repeatedly querying a module while it is being deployed.
	 * Default value is one minute, being queried every 5 seconds.
	 */
	protected Attempts deploymentTimeout() {
		return new Attempts(12, 5000);
	}

	/**
	 * Return the timeout to use for repeatedly querying a module while it is being un-deployed.
	 * Default value is one minute, being queried every 5 seconds.
	 */
	protected Attempts undeploymentTimeout() {
		return new Attempts(12, 5000);
	}

	/**
	 * Represents a timeout for querying status, with repetitive queries until a certain number have been made.
	 *
	 * @author Eric Bottard
	 */
	protected static class Attempts {
		public final int noAttempts;
		public final int pause;

		public Attempts(int noAttempts, int pause) {
			this.noAttempts = noAttempts;
			this.pause = pause;
		}
	}

	/**
	 * A Hamcrest Matcher that queries the deployment status for some {@link ModuleDeploymentId}.
	 *
	 * @author Eric Bottard
	 */
	protected Matcher<ModuleDeploymentId> hasStatusThat(final Matcher<ModuleStatus> statusMatcher) {
		return new BaseMatcher<ModuleDeploymentId>() {

			private ModuleStatus status;

			@Override
			public boolean matches(Object item) {
				status = moduleDeployer.status((ModuleDeploymentId) item);
				return statusMatcher.matches(status);
			}

			@Override
			public void describeMismatch(Object item, Description mismatchDescription) {
				mismatchDescription.appendText("status of ").appendValue(item).appendText(" ");
				statusMatcher.describeMismatch(status, mismatchDescription);
			}


			@Override
			public void describeTo(Description description) {
				statusMatcher.describeTo(description);
			}
		};
	}

}
