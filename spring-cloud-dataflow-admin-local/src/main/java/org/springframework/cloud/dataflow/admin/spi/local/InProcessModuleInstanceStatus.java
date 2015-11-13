/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.admin.spi.local;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.module.DeploymentState;
import org.springframework.cloud.dataflow.module.ModuleInstanceStatus;

/**
 * @author Mark Fisher
 */
public class InProcessModuleInstanceStatus implements ModuleInstanceStatus {

	private static final Logger logger = LoggerFactory.getLogger(InProcessModuleInstanceStatus.class);

	private final String id;

	private final DeploymentState state;

	private final Map<String, String> attributes = new HashMap<String, String>();

	// todo: this is just a simple placeholder, providing state as 'deployed' or 'unknown'
	public InProcessModuleInstanceStatus(String id, boolean deployed, Map<String, String> attributes) {
		logger.trace("Local Module {}, deployed {}, attributes: {}", id, deployed, attributes);
		this.id = id;
		this.state = deployed ? DeploymentState.deployed : DeploymentState.unknown;
		if (attributes != null) {
			this.attributes.putAll(attributes);
		}
	}

	public String getId() {
		return id;
	}

	public DeploymentState getState() {
		return state;
	}

	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

}
