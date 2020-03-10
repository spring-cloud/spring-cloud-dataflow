/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.core;

/**
 * @author David Turanski
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 **/
public interface TaskPlatformFactory {

	String CLOUDFOUNDRY_PLATFORM_TYPE = "Cloud Foundry";
	String KUBERNETES_PLATFORM_TYPE = "Kubernetes";
	String LOCAL_PLATFORM_TYPE = "Local";

	/**
	 * Create the {@link TaskPlatform} instance with the launchers.
	 *
	 * @return the task platform
	 */
	TaskPlatform createTaskPlatform();

	/**
	 * Create the {@link Launcher} by the given name.
	 *
	 * @param account the name of the launcher
	 * @return the launcher instance.
	 */
	Launcher createLauncher(String account);
}
