/*
 * Copyright 2015-2017 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.configuration.metadata.container.RegistryConfiguration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Christian Tzolov
 */
public class KubernetesSecretToRegistryConfigurationConverter implements Converter<String, Map<String, RegistryConfiguration>> {

	private static final Logger logger = LoggerFactory.getLogger(KubernetesSecretToRegistryConfigurationConverter.class);

	private RestTemplate restTemplate;

	public KubernetesSecretToRegistryConfigurationConverter(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	/**
	 * {"auths":{"demo.goharbor.io":{"username":"admin","password":"Harbor12345","auth":"YWRtaW46SGFyYm9yMTIzNDU="}}}
	 *
	 * @param dockerconfigjson
	 * @return
	 */
	@Override
	public Map<String, RegistryConfiguration> convert(String dockerconfigjson) {

		if (StringUtils.hasText(dockerconfigjson)) {
			try {
				Map authsMap = (Map) new ObjectMapper().readValue(dockerconfigjson, Map.class).get("auths");

				Map<String, RegistryConfiguration> registryConfigurationMap = new HashMap<>();
				for (Object registryUrl : authsMap.keySet()) {
					RegistryConfiguration rc = new RegistryConfiguration();
					rc.setRegistryHost(registryUrl.toString());
					Map registryMap = (Map) authsMap.get(registryUrl.toString());
					rc.setUser((String) registryMap.get("username"));
					rc.setSecret((String) registryMap.get("password"));

					String tokenAccessUrl = determineAuthorizationType(rc.getRegistryHost(), rc.getUser(), rc.getSecret());
					if (StringUtils.isEmpty(tokenAccessUrl)) {
						rc.setAuthorizationType(RegistryConfiguration.AuthorizationType.basicauth);
					}
					else {
						rc.setAuthorizationType(RegistryConfiguration.AuthorizationType.dockerhub);
						rc.getExtra().put("registryAuthUri", tokenAccessUrl);
					}

					logger.info("Registry Secret: " + rc.toString());

					registryConfigurationMap.put(rc.getRegistryHost(), rc);
				}
				return registryConfigurationMap;
			}
			catch (Exception e) {
				logger.error("Failed to parse the Secret:" + dockerconfigjson);
			}
		}
		return Collections.emptyMap();
	}

	/**
	 * @param registryHost
	 * @param username
	 * @param password
	 * @return Returns Token Endpoint Url if dockerhub authorization-type or null for basic auth.
	 */
	private String determineAuthorizationType(String registryHost, String username, String password) {

		final HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setBasicAuth(username, password);

		try {
			this.restTemplate.exchange(
					UriComponentsBuilder.newInstance().scheme("https").host(registryHost).path("v2/_catalog").build().toUri(),
					HttpMethod.GET,
					new HttpEntity<>(httpHeaders),
					Map.class);
			return null;
		}
		catch (HttpClientErrorException httpError) {

			if (httpError.getRawStatusCode() != 401) {
				return null;
			}
			if (!httpError.getResponseHeaders().containsKey("Www-Authenticate")) {
				return null; // Not Docker OAuth2
			}

			List<String> wwwAuthenticate = httpError.getResponseHeaders().get("Www-Authenticate");
			logger.info("" + wwwAuthenticate);

			Map<String, String> wwwAuthenticateAttributes = Stream.of(wwwAuthenticate.get(0).split(","))
					.map(s -> s.split("="))
					.collect(Collectors.toMap(b -> b[0], b -> b[1]));

			String tokenServiceUri = String.format("%s?service=%s&scope=repository:{repository}:pull",
					wwwAuthenticateAttributes.get("Bearer realm"), wwwAuthenticateAttributes.get("service"));

			// clear redundant quotes.
			tokenServiceUri = tokenServiceUri.replaceAll("\"", "");

			logger.info("tokenServiceUri: " + tokenServiceUri);

			return tokenServiceUri;
		}
		catch (Exception e) {
			return null;
		}
	}
}
