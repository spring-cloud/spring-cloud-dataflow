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

package org.springframework.cloud.dataflow.module.deployer.yarn;

import java.util.HashMap;
import java.util.Map;

import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus;
import org.springframework.cloud.dataflow.module.ModuleStatus.State;

/**
 * {@link ModuleInstanceStatus} for modules deployed into yarn.
 *
 * @author Janne Valkealahti
 *
 */
public class YarnModuleInstanceStatus implements ModuleInstanceStatus {

	private final String id;

	private final ModuleStatus.State state;

	private final Map<String, String> attributes = new HashMap<String, String>();

	public YarnModuleInstanceStatus(String id, boolean deployed, Map<String, String> attributes) {
		this.id = id;
		this.state = deployed ? ModuleStatus.State.deployed : ModuleStatus.State.unknown;
		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public Map<String, String> getAttributes() {
		return attributes;
	}

}
