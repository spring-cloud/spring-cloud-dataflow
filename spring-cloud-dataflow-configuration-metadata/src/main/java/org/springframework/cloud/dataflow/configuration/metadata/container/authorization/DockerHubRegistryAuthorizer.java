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

package org.springframework.cloud.dataflow.configuration.metadata.container.authorization;

import java.util.Map;

import org.springframework.cloud.dataflow.configuration.metadata.container.ContainerImage;
import org.springframework.cloud.dataflow.configuration.metadata.container.RegistryConfiguration;
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
public class DockerHubRegistryAuthorizer implements RegistryAuthorizer {

	public static final String DEFAULT_DOCKER_REGISTRY_AUTH_URI = "https://auth.docker.io/token?service=registry.docker.io&scope=repository:{repository}:pull&offline_token=1&client_id=shell";

	public static final String TOKEN_KEY = "token";
	public static final String DOCKER_REGISTRY_AUTH_URI_KEY = "registryAuthUri";

	@Override
	public RegistryConfiguration.AuthorizationType getType() {
		return RegistryConfiguration.AuthorizationType.dockeroauth2;
	}

	@Override
	public HttpHeaders getAuthorizationHeaders(ContainerImage containerImage, RegistryConfiguration registryConfiguration) {

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

		String registryAuthUri = registryConfiguration.getExtra().containsKey(DOCKER_REGISTRY_AUTH_URI_KEY) ?
				registryConfiguration.getExtra().get(DOCKER_REGISTRY_AUTH_URI_KEY) : DEFAULT_DOCKER_REGISTRY_AUTH_URI;
		UriComponents uriComponents = UriComponentsBuilder.newInstance()
				.fromHttpUrl(registryAuthUri).build().expand(imageRepository);

		final HttpEntity<String> entity = new HttpEntity<>(requestHttpHeaders);
		ResponseEntity<Map> authorization = new RestTemplate().exchange(uriComponents.toUri(),
				HttpMethod.GET, entity, Map.class);
		Map<String, String> authorizationBody = (Map<String, String>) authorization.getBody();

		final HttpHeaders responseHttpHeaders = new HttpHeaders();
		responseHttpHeaders.setBearerAuth(authorizationBody.get(TOKEN_KEY));
		return responseHttpHeaders;
	}
}
