/*
 * Copyright 2015-2021 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.container.registry.ContainerImageRestTemplateFactory;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryConfiguration;
import org.springframework.cloud.dataflow.container.registry.ContainerRegistryProperties;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Christian Tzolov
 */
public class DockerConfigJsonSecretToRegistryConfigurationConverter implements Converter<String, Map<String, ContainerRegistryConfiguration>> {

	private static final Logger logger = LoggerFactory.getLogger(DockerConfigJsonSecretToRegistryConfigurationConverter.class);
	public static final String BEARER_REALM_ATTRIBUTE = "Bearer realm";
	public static final String SERVICE_ATTRIBUTE = "service";

	//	private final RestTemplate restTemplate;
	private final ContainerImageRestTemplateFactory containerImageRestTemplate;

	private final Map<String, Boolean> httpProxyPerHost;

	public DockerConfigJsonSecretToRegistryConfigurationConverter(ContainerRegistryProperties properties,
			ContainerImageRestTemplateFactory containerImageRestTemplate) {

		// Retrieve registry configurations, explicitly declared via properties.
		this.httpProxyPerHost = properties.getRegistryConfigurations().entrySet().stream()
				.collect(Collectors.toMap(e -> e.getValue().getRegistryHost(), e -> e.getValue().isUseHttpProxy()));
		this.containerImageRestTemplate = containerImageRestTemplate;
	}

	/**
	 * The .dockerconfigjson value hast the following format:
	 * <code>
	 *  {"auths":{"demo.goharbor.io":{"username":"admin","password":"Harbor12345","auth":"YWRtaW46SGFyYm9yMTIzNDU="}}}
	 * </code>
	 *
	 * The map key is the registry host name and the value contains the username  and password to access this registry.
	 *
	 * @param dockerconfigjson to convert into RegistryConfiguration map.
	 *
	 * @return Return as (host-name, registry-configuration) map constructed from the dockerconfigjson content.
	 */
	@Override
	public Map<String, ContainerRegistryConfiguration> convert(String dockerconfigjson) {

		if (StringUtils.hasText(dockerconfigjson)) {
			try {
				Map authsMap = (Map) new ObjectMapper().readValue(dockerconfigjson, Map.class).get("auths");

				Map<String, ContainerRegistryConfiguration> registryConfigurationMap = new HashMap<>();
				for (Object registryUrl : authsMap.keySet()) {
					ContainerRegistryConfiguration rc = new ContainerRegistryConfiguration();
					rc.setRegistryHost(registryUrl.toString());
					Map registryMap = (Map) authsMap.get(registryUrl.toString());
					rc.setUser((String) registryMap.get("username"));
					rc.setSecret((String) registryMap.get("password"));

					Optional<String> tokenAccessUrl = getDockerTokenServiceUri(rc.getRegistryHost(),
							true, this.httpProxyPerHost.getOrDefault(rc.getRegistryHost(), false));

					if (tokenAccessUrl.isPresent()) {
						rc.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
						rc.getExtra().put(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY, tokenAccessUrl.get());
					}
					else {
						if (StringUtils.isEmpty(rc.getUser()) && StringUtils.isEmpty(rc.getSecret())) {
							rc.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.anonymous);
						}
						else {
							rc.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.basicauth);
						}
					}

					logger.info("Registry Configuration: " + rc.toString());

					registryConfigurationMap.put(rc.getRegistryHost(), rc);
				}
				return registryConfigurationMap;
			}
			catch (Exception e) {
				logger.error("Failed to parse the Secrets in dockerconfigjson");
			}
		}
		return Collections.emptyMap();
	}

	/**
	 * Best effort to construct a valid Docker OAuth2 token authorization uri from the HTTP 401 Error response.
	 *
	 * Hit the http://registry-host/v2/ and parse the on authorization error (401) response.
	 * If a Www-Authenticate response header exists and contains a "Bearer realm" and "service" attributes then use
	 * them to constructs the Token Endpoint URI.
	 *
	 * Returns null for non 401 errors or invalid Www-Authenticate content.
	 *
	 * Applicable only for dockeroauth2 authorization-type.
	 *
	 * @param registryHost Container Registry host to retrieve the tokenServiceUri for.
	 * @return Returns Token Endpoint Url or null.
	 */
	public Optional<String> getDockerTokenServiceUri(String registryHost, boolean disableSSl, boolean useHttpProxy) {

		try {
			RestTemplate restTemplate = this.containerImageRestTemplate.getContainerRestTemplate(disableSSl, useHttpProxy);
			restTemplate.exchange(
					UriComponentsBuilder.newInstance().scheme("https").host(registryHost).path("v2/").build().toUri(),
					HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Map.class);
			return Optional.empty();
		}
		catch (HttpClientErrorException httpError) {

			if (httpError.getRawStatusCode() != 401) {
				return Optional.empty();
			}
			if (httpError.getResponseHeaders() == null
					|| !httpError.getResponseHeaders().containsKey(HttpHeaders.WWW_AUTHENTICATE)) {
				return Optional.empty();
			}

			List<String> wwwAuthenticate = httpError.getResponseHeaders().get(HttpHeaders.WWW_AUTHENTICATE);
			logger.info("Www-Authenticate: {} for container registry {}", wwwAuthenticate, registryHost);

			if (CollectionUtils.isEmpty(wwwAuthenticate)) {
				return Optional.empty();
			}

			// Extract the "Bearer realm" and "service" attributes from the Www-Authenticate value
			Map<String, String> wwwAuthenticateAttributes = Stream.of(wwwAuthenticate.get(0).split(","))
					.map(s -> s.split("="))
					.collect(Collectors.toMap(b -> b[0], b -> b[1]));

			if (CollectionUtils.isEmpty(wwwAuthenticateAttributes)
					|| !wwwAuthenticateAttributes.containsKey(BEARER_REALM_ATTRIBUTE)
					|| !wwwAuthenticateAttributes.containsKey(SERVICE_ATTRIBUTE)) {
				logger.warn("Invalid Www-Authenticate: {} for container registry {}", wwwAuthenticate, registryHost);
				return Optional.empty();
			}

			String tokenServiceUri = String.format("%s?service=%s&scope=repository:{repository}:pull",
					wwwAuthenticateAttributes.get(BEARER_REALM_ATTRIBUTE), wwwAuthenticateAttributes.get(SERVICE_ATTRIBUTE));

			// remove redundant quotes.
			tokenServiceUri = tokenServiceUri.replaceAll("\"", "");

			logger.info("tokenServiceUri: " + tokenServiceUri);

			return Optional.of(tokenServiceUri);
		}
		catch (Exception e) {
			return Optional.empty();
		}
	}
}
