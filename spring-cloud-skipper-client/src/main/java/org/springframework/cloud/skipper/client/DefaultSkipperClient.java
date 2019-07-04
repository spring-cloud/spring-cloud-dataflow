/*
 * Copyright 2017-2019 the original author or authors.
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
package org.springframework.cloud.skipper.client;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.CancelRequest;
import org.springframework.cloud.skipper.domain.CancelResponse;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.Resources;
import org.springframework.hateoas.client.Traverson;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestOperations;
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
		this.traverson = createTraverson(baseUri, restTemplate);
		this.baseUri = baseUri;
		this.restTemplate = restTemplate;
	}

	@Override
	public Template getSpringCloudDeployerApplicationTemplate() {
		org.springframework.core.io.Resource resource = new ClassPathResource(
				"/org/springframework/cloud/skipper/io/generic-template.yml");
		String genericTempateData = null;
		try {
			genericTempateData = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Can't load generic template", e);
		}
		Template template = new Template();
		template.setData(genericTempateData);
		try {
			template.setName(resource.getURL().toString());
		}
		catch (IOException e) {
			log.error("Could not get URL of resource " + resource.getDescription(), e);
			throw new SkipperServerException("Could not get URL of resource " + resource.getDescription(), e);
		}
		return template;
	}

	@Override
	public AboutResource info() {
		return this.restTemplate.getForObject(baseUri + "/about", AboutResource.class);
	}

	@Override
	public Info status(String releaseName) {
		ParameterizedTypeReference<Resource<Info>> typeReference =
				new ParameterizedTypeReference<Resource<Info>>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);

		ResponseEntity<Resource<Info>> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/status/{releaseName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody().getContent();
	}

	@Override
	public Info status(String releaseName, int releaseVersion) {
		ParameterizedTypeReference<Resource<Info>> typeReference =
				new ParameterizedTypeReference<Resource<Info>>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("releaseVersion", Integer.toString(releaseVersion));
		ResponseEntity<Resource<Info>> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/status/{releaseName}/{releaseVersion}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody().getContent();
	}

	@Override
	public String getLog(String releaseName) {
		ParameterizedTypeReference<String> typeReference =
				new ParameterizedTypeReference<String>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);

		ResponseEntity<String> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/logs/{releaseName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody();
	}

	@Override
	public String getLog(String releaseName, String appName) {
		ParameterizedTypeReference<String> typeReference =
				new ParameterizedTypeReference<String>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("appName", appName);

		ResponseEntity<String> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/logs/{releaseName}/{appName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody();
	}

	@Override
	public String manifest(String releaseName) {
		ParameterizedTypeReference<Resource<Manifest>> typeReference =
				new ParameterizedTypeReference<Resource<Manifest>>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		ResponseEntity<Resource<Manifest>> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/manifest/{releaseName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody().getContent().getData();
	}

	@Override
	public String manifest(String releaseName, int releaseVersion) {
		ParameterizedTypeReference<Resource<Manifest>> typeReference =
				new ParameterizedTypeReference<Resource<Manifest>>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("releaseVersion", Integer.toString(releaseVersion));
		ResponseEntity<Resource<Manifest>> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/manifest/{releaseName}/{releaseVersion}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody().getContent().getData();
	}

	@Override
	public Resources<PackageMetadata> search(String name, boolean details) {
		ParameterizedTypeReference<Resources<PackageMetadata>> typeReference =
				new ParameterizedTypeReference<Resources<PackageMetadata>>() { };
		Traverson.TraversalBuilder traversalBuilder = this.traverson.follow("packageMetadata");
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("size", 2000);
		if (StringUtils.hasText(name)) {
			parameters.put("name", name);
			traversalBuilder.follow("search", "findByNameContainingIgnoreCase");
		}
		if (!details) {
			parameters.put("projection", "summary");
			parameters.put("sort", "name,asc");
		}
		return traversalBuilder.withTemplateParameters(parameters).toObject(typeReference);
	}

	public Release install(InstallRequest installRequest) {
		ParameterizedTypeReference<Resource<Release>> typeReference =
				new ParameterizedTypeReference<Resource<Release>>() { };
		String url = String.format("%s/%s/%s", baseUri, "package", "install");

		HttpEntity<InstallRequest> httpEntity = new HttpEntity<>(installRequest);
		ResponseEntity<Resource<Release>> resourceResponseEntity =
				restTemplate.exchange(url, HttpMethod.POST, httpEntity, typeReference);
		return resourceResponseEntity.getBody().getContent();
	}

	@Override
	public Release upgrade(UpgradeRequest upgradeRequest) {
		String url = String.format("%s/%s/%s", baseUri, "release", "upgrade");
		log.debug("Posting UpgradeRequest to " + url + ". UpgradeRequest = " + upgradeRequest);
		return this.restTemplate.postForObject(url, upgradeRequest, Release.class);
	}

	@Override
	public void delete(String releaseName, boolean deletePackage) {
		String url = null;
		if (deletePackage) {
			url = String.format("%s/%s/%s/%s", baseUri, "release", releaseName, "package");
			log.debug("Sending Delete request to " + url + " with the option to delete package");
		}
		else {
			url = String.format("%s/%s/%s", baseUri, "release", releaseName);
		}
		this.restTemplate.delete(url, deletePackage);
	}

	@Override
	public CancelResponse cancel(CancelRequest cancelRequest) {
		String url = String.format("%s/%s/%s", baseUri, "release", "cancel");
		log.debug("Posting CancelRequest to " + url + ". CancelRequest = " + cancelRequest);
		return this.restTemplate.postForObject(url, cancelRequest, CancelResponse.class);
	}

	@Override
	public Release rollback(RollbackRequest rollbackRequest) {
		ParameterizedTypeReference<Resource<Release>> typeReference =
				new ParameterizedTypeReference<Resource<Release>>() { };
		String url = String.format("%s/%s/%s", baseUri, "release", "rollback");

		HttpEntity<RollbackRequest> httpEntity = new HttpEntity<>(rollbackRequest);
		ResponseEntity<Resource<Release>> resourceResponseEntity =
				restTemplate.exchange(url, HttpMethod.POST, httpEntity, typeReference);
		return resourceResponseEntity.getBody().getContent();
	}

	@Override
	public Release rollback(String releaseName, int releaseVersion) {
		return rollback(new RollbackRequest(releaseName, releaseVersion));
	}

	@Override
	public List<Release> list(String releaseNameLike) {
		ParameterizedTypeReference<Resources<Release>> typeReference = new ParameterizedTypeReference<Resources<Release>>() {
		};
		String url;
		if (StringUtils.hasText(releaseNameLike)) {
			url = String.format("%s/%s/%s/%s", baseUri, "release", "list", releaseNameLike);
		}
		else {
			url = String.format("%s/%s/%s", baseUri, "release", "list");
		}
		return this.restTemplate.exchange(url, HttpMethod.GET, null, typeReference, new HashMap<>())
				.getBody().getContent().stream().collect(Collectors.toList());
	}

	@Override
	public Resources<Release> history(String releaseName) {
		ParameterizedTypeReference<Resources<Release>> typeReference =
				new ParameterizedTypeReference<Resources<Release>>() { };
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
	public Resources<Deployer> listDeployers() {
		ParameterizedTypeReference<Resources<Deployer>> typeReference = new ParameterizedTypeReference<Resources<Deployer>>() {
		};
		Traverson.TraversalBuilder traversalBuilder = this.traverson.follow("deployers");
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("size", 2000);
		return traversalBuilder.withTemplateParameters(parameters).toObject(typeReference);
	}

	@Override
	public PackageMetadata upload(UploadRequest uploadRequest) {
		ParameterizedTypeReference<Resource<PackageMetadata>> typeReference =
				new ParameterizedTypeReference<Resource<PackageMetadata>>() { };
		String url = String.format("%s/%s/%s", baseUri, "package", "upload");
		log.debug("Uploading package {}-{} to repository {}.", uploadRequest.getName(), uploadRequest.getVersion(),
				uploadRequest.getRepoName());
		HttpEntity<UploadRequest> httpEntity = new HttpEntity<>(uploadRequest);
		ResponseEntity<Resource<PackageMetadata>> resourceResponseEntity =
				restTemplate.exchange(url, HttpMethod.POST, httpEntity, typeReference);
		PackageMetadata packageMetadata = resourceResponseEntity.getBody().getContent();
		return packageMetadata;
	}

	@Override
	public void packageDelete(String packageName) {
		String url = String.format("%s/%s/%s", baseUri, "package", packageName);
		this.restTemplate.delete(url);
	}

	protected Traverson createTraverson(String baseUrl, RestOperations restOperations) {
		try {
			return new Traverson(new URI(baseUrl), MediaTypes.HAL_JSON).setRestOperations(restOperations);
		}
		catch (URISyntaxException e) {
			throw new IllegalStateException("Bad URI syntax: " + baseUrl);
		}
	}

}
