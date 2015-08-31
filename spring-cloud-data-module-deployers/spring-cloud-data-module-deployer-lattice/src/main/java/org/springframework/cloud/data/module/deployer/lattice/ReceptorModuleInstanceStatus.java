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

package org.springframework.cloud.data.module.deployer.lattice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.data.module.ModuleInstanceStatus;
import org.springframework.cloud.data.module.ModuleStatus;

/**
 * @author Patrick Peralta
 * @author Michael Minella
 */
public class ReceptorModuleInstanceStatus implements ModuleInstanceStatus {
	private static final Logger logger = LoggerFactory.getLogger(ReceptorModuleInstanceStatus.class);

	private final String id;

	private final ModuleStatus.State state;

	private final Map<String, String> attributes = new HashMap<String, String>();

	public ReceptorModuleInstanceStatus(String id, ModuleStatus.State state, Map<String, String> attributes) {
		logger.trace("ModuleStatus {}, state {}, attributes: {}", id, state, attributes);

		this.id = id;
		this.attributes.putAll(attributes);
		this.state = state;
	}

	public String getId() {
		return id;
	}

	public ModuleStatus.State getState() {
		return state;
	}

	public Map<String, String> getAttributes() {
		return Collections.unmodifiableMap(attributes);
	}

}
