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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.cloudfoundry.client.lib.domain.InstanceInfo;
import org.cloudfoundry.client.lib.domain.InstanceState;
import org.cloudfoundry.client.lib.domain.InstanceStats;

import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.util.StringUtils;

/**
 * Adapter from the structures returned by the Cloud Foundry client API to ModuleInstanceStatus.
 *
 * <p>May also represent a non-existing instance.</p>
 *
 * @author Eric Bottard
 */
public class CloudFoundryModuleInstanceStatus implements ModuleInstanceStatus {

	private final String applicationName;

	private final InstanceInfo instance;

	private final InstanceStats instanceStats;

	private final int index;

	/**
	 * Construct a status for an instance that should be running (but may not actually be).
	 */
	public CloudFoundryModuleInstanceStatus(String applicationName, InstanceInfo instance, InstanceStats instanceStats) {
		this.applicationName = applicationName;
		this.instance = instance;
		this.instanceStats = instanceStats;
		this.index = instance.getIndex();
	}

	/**
	 * Construct a status for an instance that does not exist (app is not running).
	 */
	public CloudFoundryModuleInstanceStatus(String applicationName, int index) {
		this.applicationName = applicationName;
		this.index = index;
		this.instance = null;
		this.instanceStats = null;
	}

	@Override
	public String getId() {
		return applicationName + ":" + index;
	}

	@Override
	public ModuleStatus.State getState() {
		return instance != null ? map(instance.getState()) : ModuleStatus.State.failed;
	}

	private ModuleStatus.State map(InstanceState state) {
		switch (state) {
			case STARTING:
			case DOWN:
				return ModuleStatus.State.deploying;
			case CRASHED:
				return ModuleStatus.State.failed;
			// Seems the client incorrectly reports apps as FLAPPING when they are
			// obviously fine. Mapping as RUNNING for now
			case FLAPPING:
			case RUNNING:
				return ModuleStatus.State.deployed;
			case UNKNOWN:
				return ModuleStatus.State.unknown;
			default:
				throw new AssertionError("Unsupported CF state " + state);
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		Map<String, String> result = new HashMap<>();
		if (instanceStats != null) {
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

			InstanceStats.Usage usage = instanceStats.getUsage();
			result.put("usage.time", formatter.format(usage.getTime()));
			result.put("usage.cpu", Double.toString(usage.getCpu()));
			result.put("usage.disk", Integer.toString(usage.getDisk()));
			result.put("usage.memory", Integer.toString(usage.getMem()));
			result.put("name", instanceStats.getName());
			result.put("uris", StringUtils.collectionToCommaDelimitedString(instanceStats.getUris()));
			result.put("host", instanceStats.getHost());
			result.put("port", Integer.toString(instanceStats.getPort()));
			result.put("uptime", Double.toString(instanceStats.getUptime()));
			result.put("mem_quota", Long.toString(instanceStats.getMemQuota()));
			result.put("disk_quota", Long.toString(instanceStats.getDiskQuota()));
			result.put("fds_quota", Integer.toString(instanceStats.getFdsQuota()));
		}
		return result;
	}
}
