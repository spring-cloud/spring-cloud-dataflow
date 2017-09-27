/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.skipper.client;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.AboutInfo;
import org.springframework.cloud.skipper.domain.InstallProperties;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * The default implementation to communicate with the Skipper Server.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 */
public class DefaultSkipperClient implements SkipperClient {

	private static final Logger log = LoggerFactory.getLogger(DefaultSkipperClient.class);

	protected final RestTemplate restTemplate;

	private final String baseUri;

	private final Traverson traverson;

	/**
	 * Create a new DefaultSkipperClient given the URL of the Server. This constructor will
	 * create a new RestTemplate instance for communication.
	 *
	 * @param baseUri the URL of the Server.
	 */
	public DefaultSkipperClient(String baseUri) {
		this(baseUri, new RestTemplate());
	}

	/**
	 * Create a new DefaultSkipperClient given the base URI of the Server and a preconfigured
	 * RestTemplate.
	 *
	 * @param baseUri the URI of the Server.
	 * @param restTemplate the template to use to make http calls to the server.
	 */
	public DefaultSkipperClient(String baseUri, RestTemplate restTemplate) {
		Assert.notNull(baseUri, "The provided baseURI must not be null.");
		Assert.notNull(restTemplate, "The provided restTemplate must not be null.");
		this.traverson = createTraverson(baseUri);
		this.baseUri = baseUri;
		this.restTemplate = restTemplate;
	}

	@Override
	public AboutInfo info() {
		return this.restTemplate.getForObject(baseUri + "/about", AboutInfo.class);
	}

	@Override
	public Resources<PackageMetadata> search(String name, boolean details) {
		ParameterizedTypeReference<Resources<PackageMetadata>> typeReference = new ParameterizedTypeReference<Resources<PackageMetadata>>() {
		};
		Traverson.TraversalBuilder traversalBuilder = this.traverson.follow("packageMetadata");
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("size", 2000);
		if (StringUtils.hasText(name)) {
			parameters.put("name", name);
			traversalBuilder.follow("search", "findByNameLike");
		}
		if (!details) {
			parameters.put("projection", "summary");
			parameters.put("sort", "name,asc");
			// TODO semver sort..
		}
		return traversalBuilder.withTemplateParameters(parameters).toObject(typeReference);
	}

	@Override
	public String install(String packageId, InstallProperties installProperties) {
		String url = String.format("%s/%s/%s", baseUri, "install", packageId);
		return this.restTemplate.postForObject(url, installProperties, String.class);
	}

	public Release install(InstallRequest installRequest) {
		String url = String.format("%s/%s", baseUri, "install");
		return this.restTemplate.postForObject(url, installRequest, Release.class);
	}

	@Override
	public Release upgrade(UpgradeRequest upgradeRequest) {
		String url = String.format("%s/%s", baseUri, "upgrade");
		return this.restTemplate.postForObject(url, upgradeRequest, Release.class);
	}

	@Override
	public Release delete(String releaseName) {
		String url = String.format("%s/%s/%s", baseUri, "delete", releaseName);
		return this.restTemplate.postForObject(url, null, Release.class);
	}

	@Override
	public Release rollback(String releaseName, int releaseVersion) {
		String url = String.format("%s/%s/%s/%s", baseUri, "rollback", releaseName, releaseVersion);
		return this.restTemplate.postForObject(url, null, Release.class);
	}

	@Override
	public List<Release> list(String releaseNameLike) {
		ParameterizedTypeReference<List<Release>> typeReference = new ParameterizedTypeReference<List<Release>>() {
		};
		String url;
		if (StringUtils.hasText(releaseNameLike)) {
			url = String.format("%s/%s/%s", baseUri, "list", releaseNameLike);
		}
		else {
			url = String.format("%s/%s", baseUri, "list");
		}
		return this.restTemplate.exchange(url, HttpMethod.GET, null, typeReference, new HashMap<>()).getBody();
	}

	@Override
	public List<Release> history(String releaseName, String maxRevisions) {
		ParameterizedTypeReference<List<Release>> typeReference = new ParameterizedTypeReference<List<Release>>() {
		};
		Map<String, Object> parameters = new HashMap<>();
		String url = String.format("%s/%s/%s/%s", baseUri, "history", releaseName, maxRevisions);
		return this.restTemplate.exchange(url, HttpMethod.GET, null, typeReference, parameters).getBody();
	}

	@Override
	public Resources<Release> history(String releaseName) {
		ParameterizedTypeReference<Resources<Release>> typeReference = new ParameterizedTypeReference<Resources<Release>>() {
		};
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", releaseName);
		Traverson.TraversalBuilder traversalBuilder = this.traverson.follow("releases", "search",
				"findByNameIgnoreCaseContainingOrderByNameAscVersionDesc");
		return traversalBuilder.withTemplateParameters(parameters).toObject(typeReference);
	}

	@Override
	public Repository addRepository(String name, String rootUrl, String sourceUrl) {
		String url = String.format("%s/%s", baseUri, "repositories");
		Repository repository = new Repository();
		repository.setName(name);
		repository.setUrl(rootUrl);
		repository.setSourceUrl(sourceUrl);
		return this.restTemplate.postForObject(url, repository, Repository.class);
	}

	@Override
	public void deleteRepository(String name) {
		ParameterizedTypeReference<Resource<Repository>> typeReference = new ParameterizedTypeReference<Resource<Repository>>() {
		};
		Traverson.TraversalBuilder traversalBuilder = this.traverson.follow("repositories", "search", "findByName");
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("name", name);
		Resource<Repository> repositoryResource = traversalBuilder.withTemplateParameters(parameters)
				.toObject(typeReference);
		if (repositoryResource != null) {
			this.restTemplate.delete(repositoryResource.getId().getHref());
		}
		else {
			throw new IllegalStateException("The Repository with the " + name + " doesn't exist.");
		}
	}

	@Override
	public Resources<Repository> listRepositories() {
		ParameterizedTypeReference<Resources<Repository>> typeReference = new ParameterizedTypeReference<Resources<Repository>>() {
		};
		Traverson.TraversalBuilder traversalBuilder = this.traverson.follow("repositories");
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("size", 2000);
		return traversalBuilder.withTemplateParameters(parameters).toObject(typeReference);
	}

	@Override
	public PackageMetadata upload(UploadRequest uploadRequest) {
		String url = String.format("%s/%s", baseUri, "upload");
		log.debug("Uploading package {}-{} to repository {}.", uploadRequest.getName(), uploadRequest.getVersion(),
				uploadRequest.getRepoName());
		return this.restTemplate.postForObject(url, uploadRequest, PackageMetadata.class);
	}

	protected Traverson createTraverson(String baseUrl) {
		try {
			return new Traverson(new URI(baseUrl), MediaTypes.HAL_JSON);
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Bad URI syntax: " + baseUrl);
		}
	}

}
