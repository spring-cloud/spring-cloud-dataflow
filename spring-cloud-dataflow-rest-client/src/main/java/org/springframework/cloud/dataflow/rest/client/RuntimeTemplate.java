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

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamStatusResource;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link RuntimeOperations}.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Christian Tzolov
 * @author Chris Bono
 * @author Corneil du Plessis
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
	 * Uri template for posting to app instance with url attribute.
	 */
	private final Link appUrlPostUriTemplate;

	/**
	 * Uri template for accessing runtime status of selected streams, their apps and instances.
	 */
	private final Link streamStatusUriTemplate;

	RuntimeTemplate(RestTemplate restTemplate, RepresentationModel<?> resources) {
		this.restTemplate = restTemplate;
		this.appStatusesUriTemplate = getLink("runtime/apps", resources, true);
		this.appStatusUriTemplate = getLink("runtime/apps/{appId}", resources, true);
		this.streamStatusUriTemplate = getLink("runtime/streams/{streamNames}", resources, true);
		this.appActuatorUriTemplate = getLink("runtime/apps/{appId}/instances/{instanceId}/actuator", resources, false);
		this.appUrlPostUriTemplate = getLink("runtime/apps/{appId}/instances/{instanceId}/post", resources, false);
	}

	private Link getLink(String relationPath, RepresentationModel<?> resources, boolean required) {
		Optional<Link> link = resources.getLink(relationPath);
		if (required && !link.isPresent()) {
			throw new RuntimeException("Unable to retrieve URI template for " + relationPath);
		}
		return link.orElse(null);
	}

	@Override
	public PagedModel<AppStatusResource> status() {
		String uriTemplate = this.appStatusesUriTemplate.expand().getHref();
		uriTemplate = uriTemplate + "?size=2000"; // TODO is this valid?
		return this.restTemplate.getForObject(uriTemplate, AppStatusResource.Page.class);
	}

	@Override
	public AppStatusResource status(String deploymentId) {
		return this.restTemplate.getForObject(
				appStatusUriTemplate.expand(deploymentId).getHref(),
				AppStatusResource.class
		);
	}

	@Override
	public PagedModel<StreamStatusResource> streamStatus(String... streamNames) {
		return this.restTemplate.getForObject(
				streamStatusUriTemplate.expand(streamNames).getHref(),
				StreamStatusResource.Page.class
		);
	}

	@Override
	public String getFromActuator(String appId, String instanceId, String endpoint) {
		Assert.notNull(appActuatorUriTemplate, "actuator endpoint not found");
		String uri = appActuatorUriTemplate.expand(appId, instanceId, endpoint).getHref();
		return this.restTemplate.getForObject(uri, String.class);
	}

	@Override
	public Object postToActuator(String appId, String instanceId, String endpoint, Map<String, Object> body) {
		Assert.notNull(appActuatorUriTemplate, "actuator endpoint not found");
		String uri = appActuatorUriTemplate.expand(appId, instanceId).getHref();
		ActuatorPostRequest actuatorPostRequest = new ActuatorPostRequest();
		actuatorPostRequest.setEndpoint(endpoint);
		actuatorPostRequest.setBody(body);
		return this.restTemplate.postForObject(uri, actuatorPostRequest, Object.class);
	}

	@Override
	public void postToUrl(String appId, String instanceId, byte[] data, HttpHeaders headers) {
		Assert.notNull(appUrlPostUriTemplate, "post endpoint not found");
		String uri = appUrlPostUriTemplate.expand(appId, instanceId).getHref();
		waitForUrl(uri, Duration.ofSeconds(30));
		HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);
		ResponseEntity<String> response = this.restTemplate.exchange(uri, HttpMethod.POST, entity, String.class);
		if (!response.getStatusCode().is2xxSuccessful()) {
			throw new RuntimeException("POST:exception:" + response.getStatusCode() + ":" + response.getBody());
		}
	}

	private void waitForUrl(String uri, Duration timeout) {
		// Check
		final long waitUntilMillis = System.currentTimeMillis() + timeout.toMillis();
		do {
			ResponseEntity<String> response = this.restTemplate.getForEntity(uri, String.class);
			if (!response.getStatusCode().is4xxClientError()) {
				break;
			}
			try {
				Thread.sleep(2000L);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		} while (waitUntilMillis <= System.currentTimeMillis());
	}
}
