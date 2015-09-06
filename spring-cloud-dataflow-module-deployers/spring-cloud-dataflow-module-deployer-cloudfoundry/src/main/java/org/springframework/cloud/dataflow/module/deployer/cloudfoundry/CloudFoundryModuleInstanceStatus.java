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

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;

/**
 * A simple holder for the module instance status derived from an application instance status.
 *
 * @author Steve Powell
 */
public class CloudFoundryModuleInstanceStatus implements ModuleInstanceStatus {

	private final Map<String, String> attributes = new HashMap<>();

	private volatile String id;

	private volatile ModuleStatus.State state;

	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public ModuleStatus.State getState() {
		return this.state;
	}

	@Override
	public Map<String, String> getAttributes() {
		return this.attributes;
	}

	CloudFoundryModuleInstanceStatus withAttribute(String key, String value) {
		this.attributes.put(key, value);
		return this;
	}

	CloudFoundryModuleInstanceStatus withAttributes(Map<String, String> attributes) {
		this.attributes.putAll(attributes);
		return this;
	}

	CloudFoundryModuleInstanceStatus withId(String id) {
		this.id = id;
		return this;
	}

	CloudFoundryModuleInstanceStatus withState(ModuleStatus.State state) {
		this.state = state;
		return this;
	}

	@Override
	public String toString() {
		return "CloudFoundryModuleInstanceStatus{state=" + this.getState() + ", id='" + this.getId() + "', attributes=" + this.getAttributes() + "}";
	}
}
