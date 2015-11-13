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

/**
 * Deployment states for modules and streams. These may represent the
 * state of:
 * <ul>
 *   <li>an entire stream</li>
 *   <li>the global state of a deployed module as part of a stream</li>
 *   <li>the state of a particular instance of a module, in cases where
 *   {@code module.count > 1}</li>
 * </ul>
 *
 * @author Patrick Peralta
 * @author Eric Bottard
 */
public enum DeploymentState {

	/**
	 * The stream or module is being deployed. If there are multiple modules or
	 * module instances, at least one of them is still being deployed.
	 */
	deploying,

	/**
	 * All modules have been successfully deployed.
	 */
	deployed,

	/**
	 * The stream or module is known to the system, but is not currently deployed.
	 */
	undeployed,

	/**
	 * The module completed execution. Currently used for tasks only.
	 */
	complete,

	/**
	 * In the case of multiple modules, some have successfully deployed, while
	 * others have not. This state does not apply for individual modules instances.
	 */
	partial,

	/**
	 * All modules have failed deployment.
	 */
	failed,

	/**
	 * A system error occurred trying to determine deployment status.
	 */
	error,

	/**
	 * The stream or module deployment is not known to the system.
	 */
	unknown;

}
