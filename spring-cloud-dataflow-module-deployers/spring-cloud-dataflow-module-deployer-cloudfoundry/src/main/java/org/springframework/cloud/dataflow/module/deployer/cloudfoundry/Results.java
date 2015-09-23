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

package org.springframework.cloud.dataflow.module.deployer.cloudfoundry;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Steve Powell
 */
class Results {
	/**
	 * Results from {@link CloudFoundryApplicationOperations#deleteApplication(Parameters.DeleteApplication) deleteApplication()} operation.
	 */
	static class DeleteApplication {

		private volatile boolean deleted;

		private volatile boolean found;

		boolean isDeleted() {
			return deleted;
		}

		DeleteApplication withDeleted(boolean deleted) {
			this.deleted = deleted;
			return this;
		}

		boolean isFound() {
			return found;
		}

		DeleteApplication withFound(boolean found) {
			this.found = found;
			return this;
		}
	}

	/**
	 * Results from {@link CloudFoundryApplicationOperations#getApplicationsStatus(Parameters.GetApplicationsStatus) getApplicationsStatus()} operation.
	 */
	static class GetApplicationsStatus {

		private Map<String, ApplicationStatus> applications = new HashMap<>();

		public Map<String, ApplicationStatus> getApplications() {
			return this.applications;
		}

		public GetApplicationsStatus withApplication(String name, ApplicationStatus status) {
			applications.put(name, status);
			return this;
		}
	}

	/**
	 * Results from {@link CloudFoundryApplicationOperations#pushBindAndStartApplication(Parameters.PushBindAndStartApplication) pushBindAndStartApplication()} operation.
	 */
	static class PushBindAndStartApplication {

		private boolean createSucceeded;

		public boolean isCreateSucceeded() {
			return createSucceeded;
		}

		public PushBindAndStartApplication withCreateSucceeded(boolean createSucceeded) {
			this.createSucceeded = createSucceeded;
			return this;
		}

	}
}
