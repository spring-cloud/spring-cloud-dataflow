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
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.util.Assert;

/**
 * Key used in an {@link AppDeploymentRepository}. Required to be used
 * either with {@link StreamDefinition} or {@link TaskDefinition} with
 * accompanying {@link ModuleDefinition}.
 *
 * @author Janne Valkealahti
 */
public class AppDeploymentKey {

	private final StreamDefinition streamDefinition;
	private final TaskDefinition taskDefinition;
	private final ModuleDefinition moduleDefinition;

	/**
	 * Instantiates a new app deployment key for stream.
	 *
	 * @param streamDefinition the stream definition
	 * @param moduleDefinition the module definition
	 */
	public AppDeploymentKey(StreamDefinition streamDefinition, ModuleDefinition moduleDefinition) {
		Assert.notNull(streamDefinition, "streamDefinition must be set");
		Assert.notNull(moduleDefinition, "moduleDefinition must be set");
		this.streamDefinition = streamDefinition;
		this.moduleDefinition = moduleDefinition;
		this.taskDefinition = null;
	}

	/**
	 * Instantiates a new app deployment key for task.
	 *
	 * @param taskDefinition the task definition
	 * @param moduleDefinition the module definition
	 */
	public AppDeploymentKey(TaskDefinition taskDefinition, ModuleDefinition moduleDefinition) {
		Assert.notNull(taskDefinition, "taskDefinition must be set");
		Assert.notNull(moduleDefinition, "moduleDefinition must be set");
		this.taskDefinition = taskDefinition;
		this.moduleDefinition = moduleDefinition;
		this.streamDefinition = null;
	}

	/**
	 * Gets the unique identifier for this key to be
	 * used in maps or other hashes.
	 *
	 * @return the unique id
	 */
	public String getId() {
		StringBuilder buf = new StringBuilder();
		if (streamDefinition != null) {
			buf.append(streamDefinition.getName());
			buf.append(streamDefinition.getDslText());
		} else if (taskDefinition != null) {
			buf.append(taskDefinition.getName());
			buf.append(taskDefinition.getDslText());
		}
		buf.append(moduleDefinition.getName());
		buf.append(moduleDefinition.getLabel());
		buf.append(moduleDefinition.getGroup());
		return buf.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((moduleDefinition == null) ? 0 : moduleDefinition.hashCode());
		result = prime * result + ((streamDefinition == null) ? 0 : streamDefinition.hashCode());
		result = prime * result + ((taskDefinition == null) ? 0 : taskDefinition.hashCode());
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
		if (taskDefinition == null) {
			if (other.taskDefinition != null)
				return false;
		} else if (!taskDefinition.equals(other.taskDefinition))
			return false;
		return true;
	}
}
