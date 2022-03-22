/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link RuntimeOperations}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Christian Tzolov
 * @author Chris Bono
 */
public class RuntimeTemplate implements RuntimeOperations {

	private final RestTemplate restTemplate;

	/**
	 * Uri template for accessing status of all apps.
	 */
	private final Link appStatusesUriTemplate;

	/**
	 * Uri template for accessing status of a single app.
	 */
	private final Link appStatusUriTemplate;

	/**
	 * Uri template for accessing actuator endpoint on a single app.
	 */
	private final Link appActuatorUriTemplate;

	/**
	 * Uri template for accessing runtime status of selected streams, their apps and instances.
	 */
	private final Link streamStatusUriTemplate;

	RuntimeTemplate(RestTemplate restTemplate, RepresentationModel<?> resources) {
		this.restTemplate = restTemplate;
		this.appStatusesUriTemplate = resources.getLink("runtime/apps").get();
		this.appStatusUriTemplate = resources.getLink("runtime/apps/{appId}").get();
		this.appActuatorUriTemplate = resources.getLink("runtime/apps/{appId}/instances/{instanceId}/actuator").get();
		this.streamStatusUriTemplate = resources.getLink("runtime/streams/{streamNames}").get();
	}

	@Override
	public PagedModel<AppStatusResource> status() {
		String uriTemplate = this.appStatusesUriTemplate.expand().getHref();
		uriTemplate = uriTemplate + "?size=2000";
		return this.restTemplate.getForObject(uriTemplate, AppStatusResource.Page.class);
	}

	@Override
	public AppStatusResource status(String deploymentId) {
		return this.restTemplate.getForObject(appStatusUriTemplate.expand(deploymentId).getHref(), AppStatusResource.class);
	}

	@Override
	public PagedModel<StreamStatusResource> streamStatus(String... streamNames) {
		return this.restTemplate.getForObject(streamStatusUriTemplate.expand(streamNames).getHref(),
				StreamStatusResource.Page.class);
	}

	@Override
	public String getFromActuator(String appId, String instanceId, String endpoint) {
		String uri = appActuatorUriTemplate.expand(appId, instanceId, endpoint).getHref();
		return this.restTemplate.getForObject(uri, String.class);
	}

	@Override
	public Object postToActuator(String appId, String instanceId, String endpoint, Map<String, Object> body) {
		String uri = appActuatorUriTemplate.expand(appId, instanceId).getHref();
		ActuatorPostRequest actuatorPostRequest = new ActuatorPostRequest();
		actuatorPostRequest.setEndpoint(endpoint);
		actuatorPostRequest.setBody(body);
		return this.restTemplate.postForObject(uri, actuatorPostRequest, Object.class);
	}
}
