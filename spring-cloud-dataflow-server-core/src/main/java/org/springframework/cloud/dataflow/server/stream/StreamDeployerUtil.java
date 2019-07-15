/*
 * Copyright 2018 the original author or authors.
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
package org.springframework.cloud.dataflow.server.stream;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.DeploymentState;

public class StreamDeployerUtil {

	private static final Logger logger = LoggerFactory.getLogger(StreamDeployerUtil.class);


	/**
	 * Aggregate the set of app states into a single state for a stream.
	 *
	 * @param states set of states for apps of a stream
	 * @return the stream state based on app states
	 */
	public static DeploymentState aggregateState(Set<DeploymentState> states) {
		if (states.size() == 1) {
			DeploymentState state = states.iterator().next();
			logger.debug("aggregateState: Deployment State Set Size = 1.  Deployment State " + state);
			// a stream which is known to the stream definition streamDefinitionRepository
			// but unknown to deployers is undeployed
			if (state == DeploymentState.unknown) {
				logger.debug("aggregateState: Returning " + DeploymentState.undeployed);
				return DeploymentState.undeployed;
			}
			else {
				logger.debug("aggregateState: Returning " + state);
				return state;
			}
		}
		DeploymentState result = DeploymentState.partial;
		if (states.isEmpty() || states.contains(DeploymentState.error)) {
			logger.debug("aggregateState: Returning " + DeploymentState.error);
			result = DeploymentState.error;
		}
		else if (states.contains(DeploymentState.deployed) && states.contains(DeploymentState.failed)) {
			logger.debug("aggregateState: Returning " + DeploymentState.partial);
			result = DeploymentState.partial;
		}
		else if (states.contains(DeploymentState.failed)) {
			logger.debug("aggregateState: Returning " + DeploymentState.failed);
			result = DeploymentState.failed;
		}
		else if (states.contains(DeploymentState.deploying)) {
			logger.debug("aggregateState: Returning " + DeploymentState.deploying);
			result = DeploymentState.deploying;
		}

		logger.debug("aggregateState: Returning " + DeploymentState.partial);
		return result;
	}
}
