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

package org.springframework.cloud.data.module.deployer.cloudfoundry;

/**
 * The direct operations necessary to support the ModuleDeployer functionality on CloudFoundry.
 *
 * @author Steve Powell
 */
interface CloudFoundryApplicationOperations {

	/**
	 * Deletes an application by name.
	 * @param parameters the delete application parameters
	 * @return a Response instance including whether application was found and was deleted.
	 */
	DeleteApplicationResults deleteApplication(DeleteApplicationParameters parameters);

	/**
	 * Get status (including instances) for all applications, or all applications (in our space).
	 * @param parameters the get applications parameters
	 * @return a Response instance carrying the instance status for all applications denoted, or just one.
	 */
	GetApplicationsStatusResults getApplicationsStatus(GetApplicationsStatusParameters parameters);

	/**
	 * Creates and starts an application. Is given name, bits resource, service-instances to bind and environment.
	 * @param parameters the push application parameters
	 * @return a Response instance carrying an indication of failure reason (if any).
	 */
	PushBindAndStartApplicationResults pushBindAndStartApplication(PushBindAndStartApplicationParameters parameters);

}
