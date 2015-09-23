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
 * Status of a Cloud Foundry application and all its instances.
 *
 * @author Steve Powell
 */
class ApplicationStatus {

	private volatile String id;

	private Map<String, Responses.ApplicationInstanceStatus> instances = new HashMap<>();

	public String getId() {
		return id;
	}

	public ApplicationStatus withId(String applicationId) {
		this.id = applicationId;
		return this;
	}

	public Map<String, Responses.ApplicationInstanceStatus> getInstances() {
		return instances;
	}

	public ApplicationStatus withInstances(Map<String, Responses.ApplicationInstanceStatus> instances) {
		this.instances.putAll(instances);
		return this;
	}
}
