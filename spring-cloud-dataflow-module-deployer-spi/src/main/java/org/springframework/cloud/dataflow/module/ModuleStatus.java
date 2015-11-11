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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.cloud.dataflow.core.ModuleDeploymentId;
import org.springframework.cloud.dataflow.core.ModuleDeploymentRequest;

/**
 * Status of a {@link ModuleDeploymentRequest}. This status is
 * composed of an aggregate of all individual module deployments.
 * <p>
 * Consumers of the SPI obtain module status via
 * {@link org.springframework.cloud.dataflow.module.deployer.ModuleDeployer#status},
 * whereas SPI implementations create instances of this class via
 * {@link ModuleStatus.Builder}.
 *
 * @see ModuleInstanceStatus
 *
 * @author Patrick Peralta
 */
public class ModuleStatus {

	/**
	 * Module deployment states. Unless indicated, these states
	 * may represent the state of an individual module deployment
	 * or the aggregate state of all module instances for a given
	 * module.
	 */
	public enum State {

		/**
		 * The module is being deployed. If there are multiple modules,
		 * at least one module is being deployed.
		 */
		deploying,

		/**
		 * All modules have been deployed.
		 */
		deployed,

		/**
		 * The module exited normally.
		 */
		complete,

		/**
		 * In the case of multiple modules, some have successfully deployed.
		 * This state does not apply for individual modules.
		 */
		incomplete,

		/**
		 * The module has failed to deploy. If there are multiple modules,
		 * all instances have failed deployment.
		 */
		failed,

		/**
		 * Module deployment state could not be calculated. This may be
		 * caused by an error in the SPI implementation.
		 */
		unknown
	}

	/**
	 * The key of the module this status is for.
	 */
	private final ModuleDeploymentId moduleDeploymentId;

	/**
	 * Map of {@link ModuleInstanceStatus} keyed by a unique identifier
	 * for each module deployment instance.
	 */
	private final Map<String, ModuleInstanceStatus> instances = new HashMap<String, ModuleInstanceStatus>();

	/**
	 * Construct a new {@code ModuleStatus}.
	 *
	 * @param moduleDeploymentId key of the module this status is for
	 */
	protected ModuleStatus(ModuleDeploymentId moduleDeploymentId) {
		this.moduleDeploymentId = moduleDeploymentId;
	}

	/**
	 * Return the module deployment id for the module.
	 * @return module deployment id
	 */
	public ModuleDeploymentId getModuleDeploymentId() {
		return moduleDeploymentId;
	}

	/**
	 * Return the deployment state for the the module. If the descriptor
	 * indicates multiple instances, this state represents an aggregate
	 * of all individual instances.
	 *
	 * @return deployment state for the module
	 */
	public State getState() {
		Set<State> instanceStates = new HashSet<State>();
		for (Map.Entry<String, ModuleInstanceStatus> entry : instances.entrySet()) {
			instanceStates.add(entry.getValue().getState());
		}
		State state = State.unknown;
		if (instanceStates.size() == 1 && instanceStates.contains(State.deployed)) {
			state = State.deployed;
		}
		if (instanceStates.contains(State.deploying)) {
			state = State.deploying;
		}
		if (instanceStates.contains(State.failed)) {
			state = (instanceStates.size() == 1 ? State.failed : State.incomplete);
		}
		return state;
	}

	/**
	 * Return a map of {@code ModuleInstanceStatus} keyed by a unique identifier
	 * for each module deployment instance.
	 *
	 * @return map of {@code ModuleInstanceStatus}
	 */
	public Map<String, ModuleInstanceStatus> getInstances() {
		return Collections.unmodifiableMap(this.instances);
	}

	private void addInstance(String id, ModuleInstanceStatus status) {
		this.instances.put(id, status);
	}

	/**
	 * Return a {@code Builder} for {@code ModuleStatus}.
	 *
	 * @param key of the module this status is for
	 *
	 * @return {@code Builder} for {@code ModuleStatus}
	 */
	public static Builder of(ModuleDeploymentId key) {
		return new Builder(key);
	}


	public static class Builder {

		private final ModuleStatus status;

		private Builder(ModuleDeploymentId key) {
			this.status = new ModuleStatus(key);
		}

		/**
		 * Add an instance of {@code ModuleInstanceStatus} to build
		 * the status for the module. This will
		 * be invoked once per individual module instance.
		 *
		 * @param instance status of individual module deployment
		 *
		 * @return this {@code Builder}
		 */
		public Builder with(ModuleInstanceStatus instance) {
			status.addInstance(instance.getId(), instance);
			return this;
		}

		/**
		 * Return a new instance of {@code ModuleStatus} based on
		 * the provided individual module instances via
		 * {@link #with(ModuleInstanceStatus)}.
		 *
		 * @return new instance of {@code ModuleStatus}
		 */
		public ModuleStatus build() {
			return status;
		}
	}

}
