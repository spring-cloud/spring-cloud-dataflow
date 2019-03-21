/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.dataflow.server.repository;

import org.springframework.cloud.dataflow.core.StreamAppDefinition;
import org.springframework.cloud.dataflow.core.TaskDefinition;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.util.Assert;

/**
 * Utility methods for determining the key to be used in a {@link DeploymentIdRepository}.
 *
 * @author Janne Valkealahti
 * @author Mark Fisher
 */
public abstract class DeploymentKey {

	/**
	 * Determines a deployment key for a stream application.
	 *
	 * @param streamAppDefinition the stream application definition
	 * @return the deployment key
	 */
	public static String forStreamAppDefinition(StreamAppDefinition streamAppDefinition) {
		Assert.notNull(streamAppDefinition, "streamAppDefinition must not be null");
		return String.format("%s.%s", streamAppDefinition.getStreamName(), streamAppDefinition.getName());
	}

	public static String forAppDeploymentRequest(String streamName, AppDefinition appDefinition) {
		Assert.notNull(streamName, "streamName must not be null");
		Assert.notNull(appDefinition, "appDefinition must not be null");
		return String.format("%s.%s", streamName, appDefinition.getName());
	}

	/**
	 * Determines a deployment key for a task application.
	 *
	 * @param taskDefinition the task application definition
	 * @return the deployment key
	 */
	public static String forTaskDefinition(TaskDefinition taskDefinition) {
		Assert.notNull(taskDefinition, "taskDefinition must not be null");
		return String.format("%s.%s", taskDefinition.getRegisteredAppName(), taskDefinition.getName());
	}
}
