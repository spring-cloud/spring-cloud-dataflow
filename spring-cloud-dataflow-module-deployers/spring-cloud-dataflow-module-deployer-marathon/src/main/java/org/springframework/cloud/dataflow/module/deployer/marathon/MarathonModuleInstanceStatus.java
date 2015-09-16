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

package org.springframework.cloud.dataflow.module.deployer.marathon;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Task;

import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.util.StringUtils;

/**
 * Adapts from the Marathon task API to ModuleInstanceStatus. An instance of this class
 * can also represent a missing application instance.
 *
 * @author Eric Bottard
 */
public class MarathonModuleInstanceStatus implements ModuleInstanceStatus {

	private final App app;

	private final Task task;

	private MarathonModuleInstanceStatus(App app, Task task) {
		this.app = app;
		this.task = task;
	}

	/**
	 * Construct a status from a running app task.
	 */
	static MarathonModuleInstanceStatus up(App app, Task task) {
		return new MarathonModuleInstanceStatus(app, task);
	}

	/**
	 * Construct a status from a missing app task (maybe it crashed, maybe Mesos could not offer enough resources, etc.)
	 */
	static MarathonModuleInstanceStatus down(App app) {
		return new MarathonModuleInstanceStatus(app, null);
	}

	@Override
	public String getId() {
		return task != null ? task.getId() : (app.getId() + "-failed-" + new Random().nextInt());
	}

	@Override
	public ModuleStatus.State getState() {
		return task != null ? ModuleStatus.State.deployed : ModuleStatus.State.failed;
	}

	@Override
	public Map<String, String> getAttributes() {
		HashMap<String, String> result = new HashMap<>();
		if (task != null) {
			result.put("staged_at", task.getStagedAt());
			result.put("started_at", task.getStartedAt());
			result.put("host", task.getHost());
			result.put("ports", StringUtils.collectionToCommaDelimitedString(task.getPorts()));
		}
		return result;
	}
}
