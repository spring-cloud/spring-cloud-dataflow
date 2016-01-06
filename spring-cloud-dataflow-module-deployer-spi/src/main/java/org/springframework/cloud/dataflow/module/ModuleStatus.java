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
 * @author Patrick Peralta
 * @see ModuleInstanceStatus
 */
public class ModuleStatus {

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
	public DeploymentState getState() {
		Set<DeploymentState> states = new HashSet<>();
		for (Map.Entry<String, ModuleInstanceStatus> entry : instances.entrySet()) {
			states.add(entry.getValue().getState());
		}

		if (states.size() == 0) {
			return DeploymentState.unknown;
		}
		if (states.size() == 1) {
			return states.iterator().next();
		}
		if (states.contains(DeploymentState.error)) {
			return DeploymentState.error;
		}
		if (states.contains(DeploymentState.deploying)) {
			return DeploymentState.deploying;
		}
		if (states.contains(DeploymentState.deployed) || states.contains(DeploymentState.partial)) {
			return DeploymentState.partial;
		}
		if (states.contains(DeploymentState.failed)) {
			return DeploymentState.failed;
		}

		// reaching here is unlikely; it would require some
		// combination of unknown, undeployed, complete
		return DeploymentState.partial;
	}

	/**
	 * Return a map of {@code ModuleInstanceStatus} keyed by a unique identifier
	 * for each module deployment instance.
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
	 * @param key of the module this status is for
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
		 * @param instance status of individual module deployment
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
		 * @return new instance of {@code ModuleStatus}
		 */
		public ModuleStatus build() {
			return status;
		}
	}

}
