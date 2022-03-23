/*
 * Copyright 2022-2022 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RuntimeTemplate}.
 *
 * @author Chris Bono
 */
class RuntimeTemplateTests {

	private RuntimeTemplate runtimeTemplate;

	private RestTemplate restTemplate;

	private RepresentationModel<?> resources;

	private final String appId = "flipflop3.log-v1";

	private final String instanceId = "flipflop3.log-v1-0";

	private final String endpoint = "info";

	@BeforeEach
	void prepareUriTemplate() {
		Link actuatorGetLink = mock(Link.class);
		when(actuatorGetLink.getHref()).thenReturn("actuator-get-link");

		Link actuatorPostLink = mock(Link.class);
		when(actuatorPostLink.getHref()).thenReturn("actuator-post-link");

		Link actuatorLink = mock(Link.class);
		when(actuatorLink.expand(appId, instanceId, endpoint)).thenReturn(actuatorGetLink);
		when(actuatorLink.expand(appId, instanceId)).thenReturn(actuatorPostLink);

		resources = mock(RepresentationModel.class);
		when(resources.getLink("runtime/apps")).thenReturn(Optional.of(mock(Link.class)));
		when(resources.getLink("runtime/apps/{appId}")).thenReturn(Optional.of(mock(Link.class)));
		when(resources.getLink("runtime/apps/{appId}/instances/{instanceId}/actuator")).thenReturn(Optional.of(actuatorLink));
		when(resources.getLink("runtime/streams/{streamNames}")).thenReturn(Optional.of(mock(Link.class)));

		restTemplate = mock(RestTemplate.class);
		runtimeTemplate = new RuntimeTemplate(restTemplate, resources);

		// Test Premise:
		// 	Mocks are constructed in manner that ensures only requests for our chosen appId/instanceId/endpoint will
		// 	result in a non-null answer to 'Link.getHref' (which is then passed into the RestTemplate).
	}

	@Test
	void getFromActuator() {
		runtimeTemplate.getFromActuator(appId, instanceId, endpoint);
		verify(restTemplate).getForObject("actuator-get-link", String.class);
	}

	@Test
	void postToActuatorWithBodyMap() {
		Map<String, Object> body = Collections.singletonMap("name", "extra");
		ActuatorPostRequest expectedPostRequest = new ActuatorPostRequest();
		expectedPostRequest.setEndpoint(endpoint);
		expectedPostRequest.setBody(body);
		runtimeTemplate.postToActuator(appId, instanceId, endpoint, body);
		verify(restTemplate).postForObject(eq("actuator-post-link"), eq(expectedPostRequest), eq(Object.class));
	}

	@Test
	void postToActuatorWithEmptyBodyMap() {
		ActuatorPostRequest expectedPostRequest = new ActuatorPostRequest();
		expectedPostRequest.setEndpoint(endpoint);
		expectedPostRequest.setBody(Collections.emptyMap());
		runtimeTemplate.postToActuator(appId, instanceId, endpoint, Collections.emptyMap());
		verify(restTemplate).postForObject(eq("actuator-post-link"), eq(expectedPostRequest), eq(Object.class));
	}

	@Test
	void postToActuatorWithNullBodyMap() {
		ActuatorPostRequest expectedPostRequest = new ActuatorPostRequest();
		expectedPostRequest.setEndpoint(endpoint);
		runtimeTemplate.postToActuator(appId, instanceId, endpoint, null);
		verify(restTemplate).postForObject(eq("actuator-post-link"), eq(expectedPostRequest), eq(Object.class));
	}

	@Test
	void appStatusesUriTemplateIsRequired() {
		when(resources.getLink("runtime/apps")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> new RuntimeTemplate(restTemplate, resources))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Unable to retrieve URI template for runtime/apps");
	}

	@Test
	void appStatusUriTemplateIsRequired() {
		when(resources.getLink("runtime/apps/{appId}")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> new RuntimeTemplate(restTemplate, resources))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Unable to retrieve URI template for runtime/apps/{appId}");
	}

	@Test
	void streamStatusUriTemplateIsRequired() {
		when(resources.getLink("runtime/streams/{streamNames}")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> new RuntimeTemplate(restTemplate, resources))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Unable to retrieve URI template for runtime/streams/{streamNames}");
	}

	@Test
	void actuatorUriTemplateIsNotRequiredForBackwardsCompatibility() {
		when(resources.getLink("runtime/apps/{appId}/instances/{instanceId}/actuator")).thenReturn(Optional.empty());
		RuntimeTemplate runtimeTemplate = new RuntimeTemplate(restTemplate, resources);
		assertThat(runtimeTemplate).hasFieldOrPropertyWithValue("appActuatorUriTemplate", null);
	}
}
