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
 * Parameters for {@link CloudFoundryApplicationOperations#getApplicationsStatus(GetApplicationsStatusParameters) getApplicationsStatus()} operation.
 * Parameter {@code name} is optional; if {@code name} is not set all applications statuses are returned.
 *
 * @author Steve Powell
 */
class GetApplicationsStatusParameters {

	private volatile String name;

	public String getName() {
		return name;
	}

	public GetApplicationsStatusParameters withName(String name) {
		this.name = name;
		return this;
	}
}
