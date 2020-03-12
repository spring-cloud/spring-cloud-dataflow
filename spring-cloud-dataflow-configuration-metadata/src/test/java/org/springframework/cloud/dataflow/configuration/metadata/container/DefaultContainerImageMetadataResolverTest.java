/*
 * Copyright 2020-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.configuration.metadata.container;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.cloud.dataflow.configuration.metadata.AppMetadataResolutionException;
import org.springframework.cloud.dataflow.configuration.metadata.container.authorization.RegistryAuthorizer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * @author Christian Tzolov
 */
public class DefaultContainerImageMetadataResolverTest {

	@Mock
	private RestTemplate mockRestTemplate;

	@Mock
	private RegistryAuthorizer registryAuthorizer;

	private ContainerImageMetadataProperties containerImageMetadataProperties;

	@Before
	public void init() {
		MockitoAnnotations.initMocks(this);

		containerImageMetadataProperties = new ContainerImageMetadataProperties();

		// DockerHub registry configuration by default.
		RegistryConfiguration dockerHubAuthConfig = new RegistryConfiguration();
		dockerHubAuthConfig.setRegistryHost(ContainerImageMetadataProperties.DOCKER_HUB_HOST);
		dockerHubAuthConfig.setAuthorizationType(RegistryConfiguration.AuthorizationType.dockerhub);

		containerImageMetadataProperties.getRegistryConfigurations().add(dockerHubAuthConfig);
	}

	@Test(expected = AppMetadataResolutionException.class)
	public void getImageLabelsInvalidImageName() {
		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), Arrays.asList(registryAuthorizer), containerImageMetadataProperties);
		resolver.getImageLabels(null);
	}

	@Test
	public void getImageLabels() {

		when(registryAuthorizer.getType()).thenReturn(RegistryConfiguration.AuthorizationType.dockerhub);
		when(registryAuthorizer.getAuthorizationHeaders(any(), any())).thenReturn(new HttpHeaders());

		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), Arrays.asList(registryAuthorizer), containerImageMetadataProperties);

		Map<String, Object> manifestResponse = Collections.singletonMap("config", Collections.singletonMap("digest", "123"));
		mockManifestRestTemplateCall(manifestResponse, "test/image", "latest");

		mockBlogRestTemplateCall("{\"config\": { \"Labels\": { \"boza\": \"koza\"} } }",
				"test/image", "123");

		Map<String, String> labels = resolver.getImageLabels("test/image:latest");
		assertThat(labels.size(), is(1));
		assertThat(labels.get("boza"), is("koza"));
	}

	@Test(expected = AppMetadataResolutionException.class)
	public void getImageLabelsMissingRegistryConfiguration() {

		when(registryAuthorizer.getType()).thenReturn(RegistryConfiguration.AuthorizationType.dockerhub);
		when(registryAuthorizer.getAuthorizationHeaders(any(), any())).thenReturn(new HttpHeaders());

		ContainerImageMetadataProperties propertiesWithoutRegistryConfiguration = new ContainerImageMetadataProperties();

		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), Arrays.asList(registryAuthorizer), propertiesWithoutRegistryConfiguration);

		resolver.getImageLabels("test/image:latest");
	}

	@Test(expected = AppMetadataResolutionException.class)
	public void getImageLabelsMissingRegistryAuthorizer() {

		List<RegistryAuthorizer> emptyAuthorizerList = Collections.emptyList();

		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), emptyAuthorizerList, containerImageMetadataProperties);

		resolver.getImageLabels("test/image:latest");
	}

	@Test(expected = AppMetadataResolutionException.class)
	public void getImageLabelsMissingAuthorizationHeader() {
		when(registryAuthorizer.getType()).thenReturn(RegistryConfiguration.AuthorizationType.dockerhub);
		when(registryAuthorizer.getAuthorizationHeaders(any(), any())).thenReturn(null);

		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), Arrays.asList(registryAuthorizer), containerImageMetadataProperties);

		resolver.getImageLabels("test/image:latest");
	}

	@Test(expected = AppMetadataResolutionException.class)
	public void getImageLabelsInvalidManifestResponse() {

		when(registryAuthorizer.getType()).thenReturn(RegistryConfiguration.AuthorizationType.dockerhub);
		when(registryAuthorizer.getAuthorizationHeaders(any(), any())).thenReturn(new HttpHeaders());

		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), Arrays.asList(registryAuthorizer), containerImageMetadataProperties);

		Map<String, Object> manifestResponseWithoutConfig = Collections.emptyMap();
		mockManifestRestTemplateCall(manifestResponseWithoutConfig, "test/image", "latest");

		resolver.getImageLabels("test/image:latest");
	}

	@Test(expected = AppMetadataResolutionException.class)
	public void getImageLabelsInvalidDigest() {
		when(registryAuthorizer.getType()).thenReturn(RegistryConfiguration.AuthorizationType.dockerhub);
		when(registryAuthorizer.getAuthorizationHeaders(any(), any())).thenReturn(new HttpHeaders());

		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), Arrays.asList(registryAuthorizer), containerImageMetadataProperties);

		String emptyDigest = "";
		Map<String, Object> manifestResponse = Collections.singletonMap("config", Collections.singletonMap("digest", emptyDigest));
		mockManifestRestTemplateCall(manifestResponse, "test/image", "latest");

		resolver.getImageLabels("test/image:latest");
	}

	@Test
	public void getImageLabelsWithInvalidLabels() {

		when(registryAuthorizer.getType()).thenReturn(RegistryConfiguration.AuthorizationType.dockerhub);
		when(registryAuthorizer.getAuthorizationHeaders(any(), any())).thenReturn(new HttpHeaders());

		DefaultContainerImageMetadataResolver resolver = new MockedDefaultContainerImageMetadataResolver(
				new ContainerImageParser(), Arrays.asList(registryAuthorizer), containerImageMetadataProperties);

		Map<String, Object> manifestResponse = Collections.singletonMap("config", Collections.singletonMap("digest", "123"));
		mockManifestRestTemplateCall(manifestResponse, "test/image", "latest");

		mockBlogRestTemplateCall("{\"config\": { } }",
				"test/image", "123");

		Map<String, String> labels = resolver.getImageLabels("test/image:latest");
		assertThat(labels.size(), is(0));
	}

	private void mockManifestRestTemplateCall(Map<String, Object> mapToReturn, String repository, String tag) {
		when(mockRestTemplate.exchange(
				eq("https://{registryHost}/v2/{repository}/manifests/{tag}"),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(Map.class),
				any(String.class),
				eq(repository),
				eq(tag))).thenReturn(
				new ResponseEntity<>(mapToReturn, HttpStatus.OK));
	}

	private void mockBlogRestTemplateCall(String jsonResponse, String repository, String digest) {
		when(mockRestTemplate.exchange(
				eq("https://{registryHost}/v2/{repository}/blobs/{digest}"),
				eq(HttpMethod.GET),
				any(HttpEntity.class),
				eq(String.class),
				any(String.class),
				eq(repository),
				eq(digest)))
				.thenReturn(new ResponseEntity<>(jsonResponse, HttpStatus.OK));
	}

	private class MockedDefaultContainerImageMetadataResolver extends DefaultContainerImageMetadataResolver {
		public MockedDefaultContainerImageMetadataResolver(ContainerImageParser containerImageParser,
				List<RegistryAuthorizer> registryAuthorizes, ContainerImageMetadataProperties registryProperties) {
			super(containerImageParser, registryAuthorizes, registryProperties);
		}

		@Override
		protected RestTemplate getRestTemplate() {
			return mockRestTemplate;
		}
	}
}
