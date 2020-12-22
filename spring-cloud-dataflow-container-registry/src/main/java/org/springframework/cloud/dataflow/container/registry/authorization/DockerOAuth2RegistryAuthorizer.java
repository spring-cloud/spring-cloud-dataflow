/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.container.registry.authorization;

import java.util.Map;

import org.springframework.cloud.dataflow.container.registry.ContainerImage;
import org.springframework.cloud.dataflow.container.registry.ContainerImageRestTemplateFactory;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryConfiguration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Docker Hub requires authorization per Repository (e.g. springcloudstream/rabbit-sink-rabbit).
 *
 * @author Christian Tzolov
 */
public class DockerOAuth2RegistryAuthorizer implements RegistryAuthorizer {

	public static final String DEFAULT_DOCKER_REGISTRY_AUTH_URI = "https://auth.docker.io/token?service=registry.docker.io&scope=repository:{repository}:pull&offline_token=1&client_id=shell";

	public static final String TOKEN_KEY = "token";

	public static final String DOCKER_REGISTRY_AUTH_URI_KEY = "registryAuthUri";

	public static final String DOCKER_REGISTRY_REPOSITORY_FIELD_KEY = "repository";

	private final ContainerImageRestTemplateFactory containerImageRestTemplate;

	public DockerOAuth2RegistryAuthorizer(
			ContainerImageRestTemplateFactory containerImageRestTemplate) {
		Assert.notNull(containerImageRestTemplate, "Non null containerImageRestTemplate is expected!");
		this.containerImageRestTemplate = containerImageRestTemplate;
	}

	@Override
	public ContainerRegistryConfiguration.AuthorizationType getType() {
		return ContainerRegistryConfiguration.AuthorizationType.dockeroauth2;
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerRegistryConfiguration registryConfiguration, Map<String, String> configProperties) {

		Assert.isTrue(registryConfiguration.getAuthorizationType() == this.getType(),
				"Incorrect authorization type: " + registryConfiguration.getAuthorizationType());

		final HttpHeaders requestHttpHeaders = new HttpHeaders();
		if (StringUtils.hasText(registryConfiguration.getUser()) && StringUtils.hasText(registryConfiguration.getSecret())) {
			// Use basic authentication to obtain the authorization token.
			// Usually the public docker hub authorization service doesn't require authentication for image pull requests.
			requestHttpHeaders.setBasicAuth(registryConfiguration.getUser(), registryConfiguration.getSecret());
		}

		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(registryConfiguration.getExtra().getOrDefault(DOCKER_REGISTRY_AUTH_URI_KEY,
				DEFAULT_DOCKER_REGISTRY_AUTH_URI)).build().expand(configProperties.get(
				DOCKER_REGISTRY_REPOSITORY_FIELD_KEY));
		final HttpHeaders responseHttpHeaders = new HttpHeaders();

		ResponseEntity<Map> authorization = this.getRestTemplate(registryConfiguration)
				.exchange(uriComponents.toUri(), HttpMethod.GET, new HttpEntity<>(requestHttpHeaders), Map.class);

		if (authorization != null) {
			Map<String, String> authorizationBody = (Map<String, String>) authorization.getBody();
			responseHttpHeaders.setBearerAuth(authorizationBody.get(TOKEN_KEY));
		}
		return responseHttpHeaders;
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerImage containerImage, ContainerRegistryConfiguration registryConfiguration) {

		Assert.isTrue(registryConfiguration.getAuthorizationType() == this.getType(),
				"Incorrect authorization type: " + registryConfiguration.getAuthorizationType());

		Assert.notNull(containerImage, "Valid containerImageName is required!");
		String imageRepository = containerImage.getRepository();
		Assert.hasText(imageRepository, "Valid repository name (e.g. namespace/repository-name without the tag)" +
				" is required for the authorization");

		final HttpHeaders requestHttpHeaders = new HttpHeaders();
		if (StringUtils.hasText(registryConfiguration.getUser()) && StringUtils.hasText(registryConfiguration.getSecret())) {
			// Use basic authentication to obtain the authorization token.
			// Usually the public docker hub authorization service doesn't require authentication for image pull requests.
			requestHttpHeaders.setBasicAuth(registryConfiguration.getUser(), registryConfiguration.getSecret());
		}

		String registryAuthUri =
				registryConfiguration.getExtra().getOrDefault(DOCKER_REGISTRY_AUTH_URI_KEY, DEFAULT_DOCKER_REGISTRY_AUTH_URI);
		UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(registryAuthUri).build().expand(imageRepository);

		final HttpHeaders responseHttpHeaders = new HttpHeaders();

		ResponseEntity<Map> authorization = this.getRestTemplate(registryConfiguration)
				.exchange(uriComponents.toUri(), HttpMethod.GET, new HttpEntity<>(requestHttpHeaders), Map.class);

		if (authorization != null) {
			Map<String, String> authorizationBody = (Map<String, String>) authorization.getBody();
			responseHttpHeaders.setBearerAuth(authorizationBody.get(TOKEN_KEY));
		}
		return responseHttpHeaders;
	}

	private RestTemplate getRestTemplate(ContainerRegistryConfiguration registryConfiguration) {
		return this.containerImageRestTemplate.getContainerRestTemplate(registryConfiguration.isDisableSslVerification(),
				registryConfiguration.isUseHttpProxy());
	}
}
