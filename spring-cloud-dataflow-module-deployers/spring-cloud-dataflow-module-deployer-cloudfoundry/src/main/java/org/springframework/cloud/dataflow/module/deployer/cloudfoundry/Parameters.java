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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;

/**
 * @author Steve Powell
 * @author Paul Harris
 */
class Parameters {
	/**
	 * Parameters for {@link CloudFoundryApplicationOperations#deleteApplication(DeleteApplication) deleteApplication()} operation.
	 */
	static class DeleteApplication {

		private volatile String name;

		public String getName() {
			return name;
		}

		public DeleteApplication withName(String name) {
			this.name = name;
			return this;
		}
	}

	/**
	 * Parameters for {@link CloudFoundryApplicationOperations#getApplicationsStatus(GetApplicationsStatus) getApplicationsStatus()} operation.
	 * Parameter {@code name} is optional; if {@code name} is {@code null} all applications statuses are requested.
	 */
	static class GetApplicationsStatus {

		private volatile String name;

		public String getName() {
			return name;
		}

		public GetApplicationsStatus withName(String name) {
			this.name = name;
			return this;
		}
	}

	/**
	 * Parameters for {@link CloudFoundryApplicationOperations#pushBindAndStartApplication(PushBindAndStartApplication) pushBindAndStartApplication()} operation.
	 */
	static class PushBindAndStartApplication {

		private Map<String, String> environment;

		private int instances = 1;

		private String name;

		private Resource resource;

		private Set<String> serviceInstanceNames = new HashSet<>();

		public Map<String, String> getEnvironment() {
			return environment;
		}

		public PushBindAndStartApplication withEnvironment(Map<String, String> environment) {
			this.environment = environment;
			return this;
		}

		public int getInstances() {
			return instances;
		}

		public PushBindAndStartApplication withInstances(int instances) {
			this.instances = instances;
			return this;
		}

		public String getName() {
			return name;
		}

		public PushBindAndStartApplication withName(String name) {
			this.name = name;
			return this;
		}

		public Resource getResource() {
			return resource;
		}

		public PushBindAndStartApplication withResource(Resource resource) {
			this.resource = resource;
			return this;
		}

		public Set<String> getServiceInstanceNames() {
			return serviceInstanceNames;
		}

		public PushBindAndStartApplication withServiceInstanceNames(Set<String> serviceInstanceNames) {
			this.serviceInstanceNames.addAll(serviceInstanceNames);
			return this;
		}
	}
}
