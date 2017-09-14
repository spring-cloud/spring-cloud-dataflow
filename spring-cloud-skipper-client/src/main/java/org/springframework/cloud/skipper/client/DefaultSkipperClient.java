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
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.AboutInfo;
import org.springframework.cloud.skipper.domain.DeployProperties;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
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

	private final String baseUrl;

	private final Traverson traverson;

	/**
	 * Create a new DefaultSkipperClient given the URL of the Server. This constructor
	 * will create a new RestTemplate instance for communication.
	 *
	 * @param baseUrl the URL of the Server.
	 */
	public DefaultSkipperClient(String baseUrl) {
		this(baseUrl, new RestTemplate());
	}

	/**
	 * Create a new DefaultSkipperClient given the URL of the Server and a preconfigured
	 * RestTemplate.
	 *
	 * @param baseUrl the URL of the Server.
	 * @param restTemplate the template to use to make http calls to the server.
	 */
	public DefaultSkipperClient(String baseUrl, RestTemplate restTemplate) {
		Assert.notNull(baseUrl, "The provided baseURI must not be null.");
		Assert.notNull(restTemplate, "The provided restTemplate must not be null.");
		this.traverson = createTraverson(baseUrl);
		this.baseUrl = baseUrl;
		this.restTemplate = restTemplate;
	}

	@Override
	public AboutInfo getAboutInfo() {
		return this.restTemplate.getForObject(baseUrl + "/about", AboutInfo.class);
	}

	@Override
	public Resources<PackageMetadata> getPackageMetadata(String name, boolean details) {
		ParameterizedTypeReference<Resources<PackageMetadata>> typeReference = new ParameterizedTypeReference<Resources<PackageMetadata>>() {
		};
		Traverson.TraversalBuilder traversalBuilder = this.traverson.follow("packageMetadata");
		Map<String, Object> parameters = new HashMap<>();
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
	public String deploy(String packageId, DeployProperties deployProperties) {
		String url = String.format("%s/%s/%s/%s", baseUrl, "package", packageId, "deploy");
		return this.restTemplate.postForObject(url, deployProperties, String.class);
	}

	@Override
	public String update(String packageId, DeployProperties deployProperties) {
		String url = String.format("%s/%s/%s/%s", baseUrl, "package", packageId, "update");
		return this.restTemplate.postForObject(url, deployProperties, String.class);
	}

	@Override
	public String undeploy(String releaseName) {
		String url = String.format("%s/%s/%s/%s", baseUrl, "release", "undeploy", releaseName);
		return this.restTemplate.postForObject(url, null, String.class);
	}

	@Override
	public String rollback(String releaseName, int releaseVersion) {
		String url = String.format("%s/%s/%s/%s/%s", baseUrl, "release", "rollback", releaseName, releaseVersion);
		return this.restTemplate.postForObject(url, null, String.class);
	}

	@Override
	public Repository addRepository(String name, String rootUrl, String sourceUrl) {
		String url = String.format("%s/%s", baseUrl, "repositories");
		Repository repository = new Repository();
		repository.setName(name);
		repository.setUrl(rootUrl);
		repository.setSourceUrl(sourceUrl);
		return this.restTemplate.postForObject(url, repository, Repository.class);
	}

	@Override
	public void deleteRepository(String name) {
		String searchUrl = String.format("%s/%s/%s", baseUrl, "repositories", "search/findByName?name=" + name);
		ResourceSupport resourceSupport = this.restTemplate.getForObject(searchUrl, ResourceSupport.class, name);
		if (resourceSupport != null) {
			this.restTemplate.delete(resourceSupport.getId().getHref());
		}
	}

	@Override
	public Resources<Repository> listRepositories() {
		String url = String.format("%s/%s", baseUrl, "repositories?size=2000");
		return this.restTemplate.getForObject(url, RepositoryResources.class);
	}

	protected Traverson createTraverson(String baseUrl) {
		try {
			return new Traverson(new URI(baseUrl), MediaTypes.HAL_JSON);
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Bad URI syntax: " + baseUrl);
		}
	}

	public static class RepositoryResources extends Resources<Repository> {

	}

}
