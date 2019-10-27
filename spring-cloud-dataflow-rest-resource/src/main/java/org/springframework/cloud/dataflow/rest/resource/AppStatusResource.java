/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;

/**
 * REST representation of an app status.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 */
public class AppStatusResource extends RepresentationModel<AppStatusResource> {

	private String deploymentId;

	private String state;

	private CollectionModel<AppInstanceStatusResource> instances;

	@SuppressWarnings("unused")
	private AppStatusResource() {
		// Noarg constructor for serialization;
	}

	public AppStatusResource(String deploymentId, String state) {
		this.deploymentId = deploymentId;
		this.state = state;
	}

	public String getName() {
		AppInstanceStatusResource instance = this.instances.iterator().next();
		return (instance != null && instance.getAttributes() != null) ?
				instance.getAttributes().get("skipper.application.name") : "no-instances";
	}

	public String getDeploymentId() {
		return deploymentId;
	}

	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public CollectionModel<AppInstanceStatusResource> getInstances() {
		return instances;
	}

	public void setInstances(CollectionModel<AppInstanceStatusResource> instances) {
		this.instances = instances;
	}

	public static class Page extends PagedModel<AppStatusResource> {

	}
}
