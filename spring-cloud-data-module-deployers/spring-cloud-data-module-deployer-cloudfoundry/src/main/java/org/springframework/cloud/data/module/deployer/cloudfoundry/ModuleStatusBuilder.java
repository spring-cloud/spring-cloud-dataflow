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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

import java.util.Map;

import org.springframework.cloud.data.core.ModuleDeploymentId;
import org.springframework.cloud.data.module.ModuleInstanceStatus;
import org.springframework.cloud.data.module.ModuleStatus;

/**
 * Build a ModuleStatus from id, {@link ApplicationStatus} and several
 * {@link ApplicationInstanceStatus} objects.
 *
 * @author Ben Hale
 * @author Steve Powell
 */
class ModuleStatusBuilder {

	private volatile ApplicationStatus applicationStatus;

	private volatile ModuleDeploymentId id;

	ModuleStatus build() {
		ModuleStatus.Builder builder = ModuleStatus.of(this.id);
		if (this.applicationStatus != null) {
			for (Map.Entry<String, ApplicationInstanceStatus> e : this.applicationStatus.getInstances().entrySet()) {
				builder.with(moduleInstanceStatus(moduleInstanceId(this.applicationStatus.getId(), e.getKey()),
						e.getValue()));
			}
		}
		return builder.build();
	}

	ModuleStatusBuilder withApplicationStatus(ApplicationStatus applicationStatus) {
		this.applicationStatus = applicationStatus;
		return this;
	}

	ModuleStatusBuilder withId(ModuleDeploymentId id) {
		this.id = id;
		return this;
	}

	// TODO: Find out what the application state candidates are
	private static ModuleStatus.State convertState(String state) {
		switch (state) {
			case "RUNNING":
				return ModuleStatus.State.deployed;
			default:
				return ModuleStatus.State.unknown;
		}
	}

	private static String moduleInstanceId(String applicationId, String index) {
		return applicationId + ":" + index;
	}

	private static ModuleInstanceStatus moduleInstanceStatus(String id,
			ApplicationInstanceStatus applicationInstanceStatus) {
		ApplicationInstanceStatus.Statistics statistics = applicationInstanceStatus.getStatistics();

		CloudFoundryModuleInstanceStatus status = new CloudFoundryModuleInstanceStatus()
				.withId(id)
				.withState(convertState(applicationInstanceStatus.getState()));

		if (statistics != null) {
			status.withAttribute("usage.time", statistics.getUsage().getTime())
					.withAttribute("usage.cpu", statistics.getUsage().getCpu().toString())
					.withAttribute("usage.time", statistics.getUsage().getDisk().toString())
					.withAttribute("usage.time", statistics.getUsage().getMemory().toString())
					.withAttribute("name", statistics.getName())
					.withAttribute("uris", statistics.getUris().toString())
					.withAttribute("host", statistics.getHost())
					.withAttribute("port", statistics.getPort().toString())
					.withAttribute("uptime", statistics.getUptime().toString())
					.withAttribute("mem_quota", statistics.getMemoryQuota().toString())
					.withAttribute("disk_quota", statistics.getDiskQuota().toString())
					.withAttribute("fds_quota", statistics.getFdsQuota().toString());
		}

		return status;
	}
}
