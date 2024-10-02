/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.dataflow.server.service.impl.validation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Lookup;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.registry.support.AppResourceCommon;
import org.springframework.cloud.dataflow.registry.support.DockerImage;
import org.springframework.cloud.dataflow.server.DockerValidatorProperties;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Provides operations to query the Docker repository for tags for a given
 * image URI.
 *
 * @author Glenn Renfro
 * @author Chris Schaefer
 */
public class DockerRegistryValidator {
	private static final Logger logger = LoggerFactory.getLogger(DockerRegistryValidator.class);
	private static final String DOCKER_REGISTRY_AUTH_TYPE = "JWT";
	private static final String DOCKER_REGISTRY_TAGS_PATH = "/%s/tags/";
	private static final String USER_NAME_KEY = "username";
	private static final String PASSWORD_KEY = "password";
	private final AppResourceCommon appResourceCommon;

	private DockerAuth dockerAuth;
	private RestTemplate restTemplate;

	private DockerResource dockerResource;
	private DockerValidatorProperties dockerValidatiorProperties;

	public DockerRegistryValidator(DockerValidatorProperties dockerValidatorProperties,
			DockerResource dockerResource) {
		this.dockerValidatiorProperties = dockerValidatorProperties;
		this.dockerResource = dockerResource;
		this.restTemplate = configureRestTemplate();
		this.dockerAuth = getDockerAuth();
		this.appResourceCommon =  new AppResourceCommon(new MavenProperties(), null);
	}

	/**
	 * Verifies that the image is present.
	 *JobDependencies.java
	 * @return true if image is present.
	 */
	public boolean isImagePresent() {
		boolean result = false;
		try {
			String resourceTag = this.appResourceCommon.getResourceVersion(this.dockerResource);
			HttpHeaders headers = new HttpHeaders();
			if (this.dockerAuth != null) {
				headers.add(HttpHeaders.AUTHORIZATION, DOCKER_REGISTRY_AUTH_TYPE + " " + this.dockerAuth.getToken());
			}
			HttpEntity<String> httpEntity = new HttpEntity<>(headers);
			String endpointUrl = getDockerTagsEndpointUrl();
			do {
				ResponseEntity tags = this.restTemplate.exchange(endpointUrl, HttpMethod.GET, httpEntity,
						DockerResult.class);
				DockerResult dockerResult = (DockerResult)tags.getBody();
				for(DockerTag dockerTag : dockerResult.getResults()) {
					if(dockerTag.getName().equals(resourceTag)) {
						result = true;
						break;
					}
				}
				endpointUrl = dockerResult.getNext();
			} while(result == false && endpointUrl != null);
		}
		catch (HttpClientErrorException hcee) {
			//when attempting to access an invalid docker image or if you
			//don't have proper credentials docker returns a 404.
			logger.info("Unable to find image because of the following exception:", hcee);
			result = false;
		}
		return result;
	}

	private RestTemplate configureRestTemplate() {
		CloseableHttpClient httpClient
				= httpClientBuilder()
				.build();
		HttpComponentsClientHttpRequestFactory requestFactory
				= new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		requestFactory.setConnectTimeout(dockerValidatiorProperties.getConnectTimeoutInMillis());
		RestTemplate restTemplate = new RestTemplate(requestFactory);
		return restTemplate;
	}


	private HttpClientBuilder httpClientBuilder() {
		// Register http/s connection factories
		Lookup<ConnectionSocketFactory> connSocketFactoryLookup = RegistryBuilder.<ConnectionSocketFactory> create()
			.register("http", new PlainConnectionSocketFactory())
			.build();
		return HttpClients.custom()
			.setConnectionManager(new BasicHttpClientConnectionManager(connSocketFactoryLookup));
	}
	private DockerAuth getDockerAuth() {
		DockerAuth result = null;
		String userName = dockerValidatiorProperties.getUserName();
		String password = dockerValidatiorProperties.getPassword();
		if (StringUtils.hasText(userName) && password != null) {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> httpEntity;
			try {
				Map<String, String> jsonMap = new HashMap<>();
				jsonMap.put(USER_NAME_KEY, userName);
				jsonMap.put(PASSWORD_KEY, password);
				ObjectMapper objectMapper = new ObjectMapper();
				String json = objectMapper.writeValueAsString(jsonMap);
				httpEntity = new HttpEntity<>(json, headers);
				ResponseEntity<DockerAuth> dockerAuth = restTemplate.exchange(
						dockerValidatiorProperties.getDockerAuthUrl(),
						HttpMethod.POST, httpEntity, DockerAuth.class);
				result = dockerAuth.getBody();
			} catch (Exception e) {
				throw new IllegalStateException("Unable to serialize jsonMap", e);
			}
		}
		return result;
	}

	private String getDockerTagsEndpointUrl() {
		return String.format(dockerValidatiorProperties.getDockerRegistryUrl() + DOCKER_REGISTRY_TAGS_PATH, getDockerImageWithoutVersion(dockerResource));
	}

	private String getDockerImageWithoutVersion(DockerResource dockerResource) {
		try {
			String uri = dockerResource.getURI().toString().substring("docker:".length());
			DockerImage dockerImage = DockerImage.fromImageName(uri);
			StringBuilder sb = new StringBuilder();
			if (StringUtils.hasText(dockerImage.getHost())) {
				sb.append(dockerImage.getHost());
				sb.append(DockerImage.SECTION_SEPARATOR);
			}
			sb.append(dockerImage.getNamespaceAndRepo());
			return sb.toString();
		}
		catch (IOException e) {
			throw new IllegalArgumentException(
					"Docker Resource URI is not in expected format to extract version. " +
							dockerResource.getDescription(), e);
		}
	}
}
