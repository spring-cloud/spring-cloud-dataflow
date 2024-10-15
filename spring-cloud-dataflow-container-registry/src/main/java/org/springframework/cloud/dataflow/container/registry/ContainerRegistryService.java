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
package org.springframework.cloud.dataflow.container.registry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.dataflow.container.registry.authorization.DockerOAuth2RegistryAuthorizer;
import org.springframework.cloud.dataflow.container.registry.authorization.RegistryAuthorizer;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Class responsible for creating registry API requests based on the parameters.
 *
 * @author Ilayaperumal Gopinathan
 * @author Christian Tzolov
 * @author Corneil du Plessis
 */
public class ContainerRegistryService {

	private static final Logger logger = LoggerFactory.getLogger(ContainerRegistryService.class);

	private static final List<String> SUPPORTED_MANIFEST_MEDIA_TYPES = Collections
		.unmodifiableList(Arrays.asList(ContainerRegistryProperties.OCI_IMAGE_MANIFEST_MEDIA_TYPE,
				ContainerRegistryProperties.DOCKER_IMAGE_MANIFEST_MEDIA_TYPE));

	private static final String HTTPS_SCHEME = "https";

	private static final String TAGS_LIST_PATH = "/v2/{repository}/tags/list";

	private static final String TAGS_FIELD = "tags";

	private static final String CATALOG_LIST_PATH = "/v2/_catalog";

	private static final String IMAGE_MANIFEST_REFERENCE_PATH = "v2/{repository}/manifests/{reference}";

	private static final String IMAGE_BLOB_DIGEST_PATH = "v2/{repository}/blobs/{digest}";

	private final ContainerImageRestTemplateFactory containerImageRestTemplateFactory;

	private final ContainerImageParser containerImageParser;

	private final Map<String, ContainerRegistryConfiguration> registryConfigurations;

	private final Map<ContainerRegistryConfiguration.AuthorizationType, RegistryAuthorizer> registryAuthorizerMap;

	public ContainerRegistryService(ContainerImageRestTemplateFactory containerImageRestTemplateFactory,
			ContainerImageParser containerImageParser,
			Map<String, ContainerRegistryConfiguration> registryConfigurations,
			List<RegistryAuthorizer> registryAuthorizers) {
		this.containerImageRestTemplateFactory = containerImageRestTemplateFactory;
		this.containerImageParser = containerImageParser;
		this.registryConfigurations = registryConfigurations;
		this.registryAuthorizerMap = new HashMap<>();
		for (RegistryAuthorizer authorizer : registryAuthorizers) {
			this.registryAuthorizerMap.put(authorizer.getType(), authorizer);
		}
	}

	public Map<String, ContainerRegistryConfiguration> getContainerRegistryConfigurations() {
		return this.registryConfigurations;
	}

	/**
	 * Get the tag information for the given container image identified by its repository
	 * and registry. The registry information is expected to be set via container registry
	 * configuration in SCDF.
	 * @param registryName the container registry name
	 * @param repositoryName the image repository name
	 * @return the list of tags for the image
	 */
	public List<String> getTags(String registryName, String repositoryName) {
		try {
			ContainerRegistryConfiguration containerRegistryConfiguration = this.registryConfigurations
				.get(registryName);
			Map<String, String> properties = new HashMap<>();
			properties.put(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_REPOSITORY_FIELD_KEY, repositoryName);
			HttpHeaders httpHeaders = new HttpHeaders(
					this.registryAuthorizerMap.get(containerRegistryConfiguration.getAuthorizationType())
						.getAuthorizationHeaders(containerRegistryConfiguration, properties));
			httpHeaders.set(HttpHeaders.ACCEPT, "application/json");

			UriComponents manifestUriComponents = UriComponentsBuilder.newInstance()
				.scheme(HTTPS_SCHEME)
				.host(containerRegistryConfiguration.getRegistryHost())
				.path(TAGS_LIST_PATH)
				.build()
				.expand(repositoryName);

			RestTemplate requestRestTemplate = this.containerImageRestTemplateFactory.getContainerRestTemplate(
					containerRegistryConfiguration.isDisableSslVerification(),
					containerRegistryConfiguration.isUseHttpProxy(), containerRegistryConfiguration.getExtra());

			ResponseEntity<Map> manifest = requestRestTemplate.exchange(manifestUriComponents.toUri(), HttpMethod.GET,
					new HttpEntity<>(httpHeaders), Map.class);
			return (List<String>) manifest.getBody().get(TAGS_FIELD);
		}
		catch (Exception e) {
			logger.error("Exception getting tag information for the {} from {}", repositoryName, registryName);
		}
		return null;
	}

	/**
	 * Get all the repositories identified by the given registry
	 * @param registryName the registry name
	 * @return the map of all the repositories
	 */
	public Map getRepositories(String registryName) {
		try {
			ContainerRegistryConfiguration containerRegistryConfiguration = this.registryConfigurations
				.get(registryName);
			Map<String, String> properties = new HashMap<>();
			properties.put(DockerOAuth2RegistryAuthorizer.DOCKER_REGISTRY_REPOSITORY_FIELD_KEY, registryName);
			HttpHeaders httpHeaders = new HttpHeaders(
					this.registryAuthorizerMap.get(containerRegistryConfiguration.getAuthorizationType())
						.getAuthorizationHeaders(containerRegistryConfiguration, properties));
			httpHeaders.set(HttpHeaders.ACCEPT, "application/json");
			UriComponents manifestUriComponents = UriComponentsBuilder.newInstance()
				.scheme(HTTPS_SCHEME)
				.host(containerRegistryConfiguration.getRegistryHost())
				.path(CATALOG_LIST_PATH)
				.build();

			RestTemplate requestRestTemplate = this.containerImageRestTemplateFactory.getContainerRestTemplate(
					containerRegistryConfiguration.isDisableSslVerification(),
					containerRegistryConfiguration.isUseHttpProxy(), containerRegistryConfiguration.getExtra());

			ResponseEntity<Map> manifest = requestRestTemplate.exchange(manifestUriComponents.toUri(), HttpMethod.GET,
					new HttpEntity<>(httpHeaders), Map.class);
			return manifest.getBody();
		}
		catch (Exception e) {
			logger.error("Exception getting repositories from {}", registryName);
		}
		return null;
	}

	public ContainerRegistryRequest getRegistryRequest(String imageName) {

		// Convert the image name into a well-formed ContainerImage
		ContainerImage containerImage = this.containerImageParser.parse(imageName);

		// Find a registry configuration that matches the image's registry host
		ContainerRegistryConfiguration registryConf = this.registryConfigurations.get(containerImage.getRegistryHost());
		if (registryConf == null) {
			throw new ContainerRegistryException(
					"Could not find an Registry Configuration for: " + containerImage.getRegistryHost());
		}

		// Retrieve a registry authorizer that supports the configured authorization type.
		RegistryAuthorizer registryAuthorizer = this.registryAuthorizerMap.get(registryConf.getAuthorizationType());
		if (registryAuthorizer == null) {
			throw new ContainerRegistryException(
					"Could not find an RegistryAuthorizer of type:" + registryConf.getAuthorizationType());
		}

		// Use the authorizer to obtain authorization headers.
		HttpHeaders authHttpHeaders = registryAuthorizer.getAuthorizationHeaders(containerImage, registryConf);
		if (authHttpHeaders == null) {
			throw new ContainerRegistryException(
					"Could not obtain authorized headers for: " + containerImage + ", config:" + registryConf);
		}

		RestTemplate requestRestTemplate = this.containerImageRestTemplateFactory.getContainerRestTemplate(
				registryConf.isDisableSslVerification(), registryConf.isUseHttpProxy(), registryConf.getExtra());

		return new ContainerRegistryRequest(containerImage, registryConf, authHttpHeaders, requestRestTemplate);
	}

	public <T> T getImageManifest(ContainerRegistryRequest registryRequest, Class<T> responseClassType) {

		String imageManifestMediaType = registryRequest.getRegistryConf().getManifestMediaType();
		if (!SUPPORTED_MANIFEST_MEDIA_TYPES.contains(imageManifestMediaType)) {
			throw new ContainerRegistryException("Not supported image manifest media type:" + imageManifestMediaType);
		}
		HttpHeaders httpHeaders = new HttpHeaders(registryRequest.getAuthHttpHeaders());
		httpHeaders.set(HttpHeaders.ACCEPT, imageManifestMediaType);

		// Docker Registry HTTP V2 API pull manifest
		ContainerImage containerImage = registryRequest.getContainerImage();
		UriComponents manifestUriComponents = UriComponentsBuilder.newInstance()
			.scheme(HTTPS_SCHEME)
			.host(containerImage.getHostname())
			.port(StringUtils.hasText(containerImage.getPort()) ? containerImage.getPort() : null)
			.path(IMAGE_MANIFEST_REFERENCE_PATH)
			.build()
			.expand(containerImage.getRepository(), containerImage.getRepositoryReference());

		ResponseEntity<T> manifest = registryRequest.getRestTemplate()
			.exchange(manifestUriComponents.toUri(), HttpMethod.GET, new HttpEntity<>(httpHeaders), responseClassType);
		return manifest.getBody();
	}

	public <T> T getImageBlob(ContainerRegistryRequest registryRequest, String configDigest,
			Class<T> responseClassType) {
		ContainerImage containerImage = registryRequest.getContainerImage();
		HttpHeaders httpHeaders = new HttpHeaders(registryRequest.getAuthHttpHeaders());

		// Docker Registry HTTP V2 API pull config blob
		UriComponents blobUriComponents = UriComponentsBuilder.newInstance()
			.scheme(HTTPS_SCHEME)
			.host(containerImage.getHostname())
			.port(StringUtils.hasText(containerImage.getPort()) ? containerImage.getPort() : null)
			.path(IMAGE_BLOB_DIGEST_PATH)
			.build()
			.expand(containerImage.getRepository(), configDigest);
		try {
			logger.info("getImageBlob:request:{},{}", blobUriComponents.toUri(), httpHeaders);
			ResponseEntity<T> blob = registryRequest.getRestTemplate()
				.exchange(blobUriComponents.toUri(), HttpMethod.GET, new HttpEntity<>(httpHeaders), responseClassType);

			return blob.getStatusCode().is2xxSuccessful() ? blob.getBody() : null;
		}
		catch (RestClientException x) {
			logger.error("getImageBlob:exception:" + x, x);
			return null;
		}
	}

}
