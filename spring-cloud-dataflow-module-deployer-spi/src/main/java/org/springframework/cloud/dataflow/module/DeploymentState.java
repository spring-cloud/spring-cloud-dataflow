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

package org.springframework.cloud.dataflow.module;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Deployment states for modules and streams. Unless indicated, these states
 * may represent the state of<ul>
 * <li>an entire stream</li>
 * <li>the global state of a deployed module as part of a stream</li>
 * <li>the state of a particular instance of a module, in cases where {@literal module.count > 1}</li>
 * </ul>
 * @author Patrick Peralta
 * @author Eric Bottard
 */
public enum DeploymentState {

	/**
	 * The stream or module is being deployed. If there are multiple modules or module instances,
	 * at least one of them is still being deployed.
	 */
	deploying,

	/**
	 * All modules have been correctly deployed.
	 */
	deployed,

	/**
	 * The stream or module is known to the system, but is not currently deployed.
	 */
	undeployed,

	/**
	 * The module exited normally.
	 */
	complete,

	/**
	 * In the case of multiple modules, some have successfully deployed, some haven't.
	 * This state does not apply for individual modules instances.
	 */
	incomplete,

	/**
	 * Reported when all components of the considered unit have failed deployment.
	 */
	failed,

	/**
	 * Used when some error occurred trying to determine deployment status.
	 */
	error,

	/**
	 * Returned when the queried stream or module deployment is not known to the system.
	 */
	unknown;

	public static DeploymentState reduce(Set<DeploymentState> states) {
		if (states.size() == 1) {
			return states.iterator().next();
		}
		if (states.contains(error)) {
			return error;
		}
		else if (states.contains(deploying)) {
			return deploying;
		}
		else if (states.contains(incomplete)) {
			return incomplete;
		}

		// deployed + any of (failed|undeployed) => incomplete
		HashSet<DeploymentState> copy = new HashSet<>(states);
		copy.retainAll(Arrays.asList(deployed, failed, undeployed));
		if (copy.equals(states)) {
			return incomplete;
		}

		throw new IllegalStateException("Could not decide how to aggregate " + states);
	}
}
