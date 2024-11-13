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

import java.net.URI;
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
 * @author Corneil du Plessis
 */
public class DockerConfigJsonSecretToRegistryConfigurationConverter implements Converter<String, Map<String, ContainerRegistryConfiguration>> {

	private static final Logger logger = LoggerFactory.getLogger(DockerConfigJsonSecretToRegistryConfigurationConverter.class);

	public static final String BEARER_REALM_ATTRIBUTE = "Bearer realm";

	public static final String SERVICE_ATTRIBUTE = "service";

	public static final String HTTPS_INDEX_DOCKER_IO_V_1 = "https://index.docker.io/v1/";

	public static final String DOCKER_IO = "docker.io";

	public static final String REGISTRY_1_DOCKER_IO = "registry-1.docker.io";

	private final ContainerImageRestTemplateFactory containerImageRestTemplateFactory;

	private final Map<String, Boolean> httpProxyPerHost;

	private final boolean replaceDefaultDockerRegistryServer;

	public DockerConfigJsonSecretToRegistryConfigurationConverter(
			ContainerRegistryProperties properties,
			ContainerImageRestTemplateFactory containerImageRestTemplateFactory) {
		this.replaceDefaultDockerRegistryServer = properties.isReplaceDefaultDockerRegistryServer();
		this.httpProxyPerHost = properties.getRegistryConfigurations().entrySet().stream()
			.collect(Collectors.toMap(e -> e.getValue().getRegistryHost(), e -> e.getValue().isUseHttpProxy()));
		this.containerImageRestTemplateFactory = containerImageRestTemplateFactory;
	}

	/**
	 * The .dockerconfigjson value hast the following format:
	 * <code>
	 * {"auths":{"demo.goharbor.io":{"username":"admin","password":"Harbor12345","auth":"YWRtaW46SGFyYm9yMTIzNDU="}}}
	 * </code>
	 * <p>
	 * The map key is the registry host name and the value contains the username  and password to access this registry.
	 *
	 * @param dockerconfigjson to convert into RegistryConfiguration map.
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
					rc.setRegistryHost(replaceDefaultDockerRegistryServerUrl(registryUrl.toString()));
					Map registryMap = (Map) authsMap.get(registryUrl.toString());
					rc.setUser((String) registryMap.get("username"));
					rc.setSecret((String) registryMap.get("password"));

					Optional<String> tokenAccessUrl = getDockerTokenServiceUri(rc.getRegistryHost(),
						true, this.httpProxyPerHost.getOrDefault(rc.getRegistryHost(), false));

					if (tokenAccessUrl.isPresent()) {
						rc.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.dockeroauth2);
						rc.getExtra().put(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_AUTH_URI_KEY, tokenAccessUrl.get());
					} else {
						if (StringUtils.isEmpty(rc.getUser()) && StringUtils.isEmpty(rc.getSecret())) {
							rc.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.anonymous);
						} else {
							rc.setAuthorizationType(ContainerRegistryConfiguration.AuthorizationType.basicauth);
						}
					}

					logger.info("Registry Configuration: " + rc.toString());

					registryConfigurationMap.put(rc.getRegistryHost(), rc);
				}
				return registryConfigurationMap;
			} catch (Exception e) {
				logger.error("Failed to parse the Secrets in dockerconfigjson");
			}
		}
		return Collections.emptyMap();
	}

	/**
	 * When the `kubectl create secret docker-registry` command is used without explicit docker-server property set
	 * the later defaults to `https://index.docker.io/v1/` (or to `domain.io`). Those secrets can be used as
	 * K8s `imagePullSecret` to pull images from Docker Hub but can not be used for SCDF Metadata Container Registry access.
	 * Later expects a docker-server=registry-1.docker.io instead.
	 * To be able to reuse docker registry secretes for the purpose of imagePullSecrets and SCDF Container Metadata retrieval.
	 * by default the `https://index.docker.io/v1/` and `domain.io` docker-server values found in any mounted dockerconfigjson secret
	 * are replaced by `registry-1.docker.io`.
	 * <p>
	 * You can override this behaviour by setting replaceDefaultDockerRegistryServer to false.
	 *
	 * @param dockerConfigJsonRegistryHost Docker-Server property value as extracted from the dockerconfigjson.
	 * @return If input url is "https://index.docker.io/v1/" or "docker.io" then return "registry-1.docker.io". Otherwise return the input url.
	 */
	private String replaceDefaultDockerRegistryServerUrl(String dockerConfigJsonRegistryHost) {
		return (this.replaceDefaultDockerRegistryServer && (DOCKER_IO.equals(dockerConfigJsonRegistryHost)
			|| HTTPS_INDEX_DOCKER_IO_V_1.equals(dockerConfigJsonRegistryHost))) ?
			REGISTRY_1_DOCKER_IO : dockerConfigJsonRegistryHost;
	}

	/**
	 * Best effort to construct a valid Docker OAuth2 token authorization uri from the HTTP 401 Error response.
	 * <p>
	 * Hit the http://registry-host/v2/ and parse the on authorization error (401) response.
	 * If a Www-Authenticate response header exists and contains a "Bearer realm" and "service" attributes then use
	 * them to constructs the Token Endpoint URI.
	 * <p>
	 * Returns null for non 401 errors or invalid Www-Authenticate content.
	 * <p>
	 * Applicable only for dockeroauth2 authorization-type.
	 *
	 * @param registryHost Container Registry host to retrieve the tokenServiceUri for.
	 * @param disableSSl Disable SSL
	 * @param useHttpProxy Enable the use of http proxy.
	 * @return Returns Token Endpoint Url or null.
	 */
	public Optional<String> getDockerTokenServiceUri(String registryHost, boolean disableSSl, boolean useHttpProxy) {

		try {
			RestTemplate restTemplate = this.containerImageRestTemplateFactory.getContainerRestTemplate(disableSSl, useHttpProxy, Collections.emptyMap());
			String host = registryHost;
			Integer port = null;
			if (registryHost.contains(":")) {
				int colon = registryHost.lastIndexOf(":");
				String portString = registryHost.substring(colon + 1);
				try {
					int intPort = Integer.parseInt(portString);
					if (Integer.toString(intPort).equals(portString) && intPort > 0 && intPort < 32767) {
						port = intPort;
						host = registryHost.substring(0, colon);
					}
				} catch (NumberFormatException x) {
					// not valid integer
				}
			}
			UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance().scheme("https").host(host);
			if (port != null) {
				uriComponentsBuilder.port(port);
			}
			uriComponentsBuilder.path("v2/");
			URI uri = uriComponentsBuilder.build().toUri();
			logger.info("getDockerTokenServiceUri:" + uri);
			restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Map.class);
			return Optional.empty();
		} catch (HttpClientErrorException httpError) {

			if (httpError.getStatusCode().value() != 401) {
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
		} catch (Exception e) {
			// Log error because we cannot change the contract that returns empty optional.
			logger.error("Ignoring:" + e, e);
			return Optional.empty();
		}
	}
}
