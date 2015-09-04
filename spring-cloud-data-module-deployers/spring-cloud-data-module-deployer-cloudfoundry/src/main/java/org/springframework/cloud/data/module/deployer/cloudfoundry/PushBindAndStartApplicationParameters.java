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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.Resource;

/**
 * Parameters for {@link CloudFoundryApplicationOperations#pushBindAndStartApplication(PushBindAndStartApplicationParameters) pushBindAndStartApplication()} operation.
 *
 * @author Steve Powell
 */
class PushBindAndStartApplicationParameters {

	private Map<String, String> environment;

	private String name;

	private Resource resource;

	private Set<String> serviceInstanceNames = new HashSet<>();

	public Map<String, String> getEnvironment() {
		return environment;
	}

	public PushBindAndStartApplicationParameters withEnvironment(Map<String, String> environment) {
		this.environment = environment;
		return this;
	}

	public String getName() {
		return name;
	}

	public PushBindAndStartApplicationParameters withName(String name) {
		this.name = name;
		return this;
	}

	public Resource getResource() {
		return resource;
	}

	public PushBindAndStartApplicationParameters withResource(Resource resource) {
		this.resource = resource;
		return this;
	}

	public Set<String> getServiceInstanceNames() {
		return serviceInstanceNames;
	}

	public PushBindAndStartApplicationParameters withServiceInstanceNames(Set<String> serviceInstanceNames) {
		this.serviceInstanceNames.addAll(serviceInstanceNames);
		return this;
	}
}
