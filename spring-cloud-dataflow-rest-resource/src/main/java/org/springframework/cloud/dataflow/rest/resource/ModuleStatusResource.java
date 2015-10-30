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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;

/**
 * REST representation of a ModuleStatus.
 *
 * @author Eric Bottard
 */
public class ModuleStatusResource extends ResourceSupport {

	private String moduleDeploymentId;

	private String state;

	private Resources<ModuleInstanceStatusResource> instances;

	private ModuleStatusResource() {
		// Noarg constructor for serialization;
	}

	public ModuleStatusResource(String moduleDeploymentId, String state) {
		this.moduleDeploymentId = moduleDeploymentId;
		this.state = state;
	}

	public String getModuleDeploymentId() {
		return moduleDeploymentId;
	}

	public String getState() {
		return state;
	}

	public Resources<ModuleInstanceStatusResource> getInstances() {
		return instances;
	}

	public void setInstances(Resources<ModuleInstanceStatusResource> instances) {
		this.instances = instances;
	}

	public static class Page extends PagedResources<ModuleStatusResource> {

	}

	public void setModuleDeploymentId(String moduleDeploymentId) {
		this.moduleDeploymentId = moduleDeploymentId;
	}

	public void setState(String state) {
		this.state = state;
	}
}
