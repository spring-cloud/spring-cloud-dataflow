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

package org.springframework.cloud.dataflow.server.repository;

import org.springframework.cloud.dataflow.core.ModuleDefinition;
import org.springframework.cloud.dataflow.core.StreamDefinition;
import org.springframework.util.Assert;

/**
 * Key used in an {@link AppDeploymentRepository}.
 *
 * @author Janne Valkealahti
 */
public class AppDeploymentKey {

	private final StreamDefinition streamDefinition;
	private final ModuleDefinition moduleDefinition;

	/**
	 * Instantiates a new app deployment key.
	 *
	 * @param streamDefinition the stream definition
	 * @param moduleDefinition the module definition
	 */
	public AppDeploymentKey(StreamDefinition streamDefinition, ModuleDefinition moduleDefinition) {
		Assert.notNull(streamDefinition, "streamDefinition must be set");
		Assert.notNull(moduleDefinition, "moduleDefinition must be set");
		this.streamDefinition = streamDefinition;
		this.moduleDefinition = moduleDefinition;
	}

	/**
	 * Gets the stream definition.
	 *
	 * @return the stream definition
	 */
	public StreamDefinition getStreamDefinition() {
		return streamDefinition;
	}

	/**
	 * Gets the module definition.
	 *
	 * @return the module definition
	 */
	public ModuleDefinition getModuleDefinition() {
		return moduleDefinition;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((moduleDefinition == null) ? 0 : moduleDefinition.hashCode());
		result = prime * result + ((streamDefinition == null) ? 0 : streamDefinition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AppDeploymentKey other = (AppDeploymentKey) obj;
		if (moduleDefinition == null) {
			if (other.moduleDefinition != null)
				return false;
		} else if (!moduleDefinition.equals(other.moduleDefinition))
			return false;
		if (streamDefinition == null) {
			if (other.streamDefinition != null)
				return false;
		} else if (!streamDefinition.equals(other.streamDefinition))
			return false;
		return true;
	}
}