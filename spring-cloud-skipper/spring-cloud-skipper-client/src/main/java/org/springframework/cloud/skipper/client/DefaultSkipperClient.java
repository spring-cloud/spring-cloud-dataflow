/*
 * Copyright 2017-2022 the original author or authors.
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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.skipper.domain.AboutResource;
import org.springframework.cloud.skipper.domain.ActuatorPostRequest;
import org.springframework.cloud.skipper.domain.CancelRequest;
import org.springframework.cloud.skipper.domain.CancelResponse;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.Info;
import org.springframework.cloud.skipper.domain.InstallRequest;
import org.springframework.cloud.skipper.domain.LogInfo;
import org.springframework.cloud.skipper.domain.Manifest;
import org.springframework.cloud.skipper.domain.PackageMetadata;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.cloud.skipper.domain.Repository;
import org.springframework.cloud.skipper.domain.RollbackRequest;
import org.springframework.cloud.skipper.domain.ScaleRequest;
import org.springframework.cloud.skipper.domain.Template;
import org.springframework.cloud.skipper.domain.UpgradeRequest;
import org.springframework.cloud.skipper.domain.UploadRequest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The default implementation to communicate with the Skipper Server.
 *
 * @author Mark Pollack
 * @author Ilayaperumal Gopinathan
 * @author Janne Valkealahti
 * @author David Turanski
 */
public class DefaultSkipperClient implements SkipperClient {

	private static final Logger log = LoggerFactory.getLogger(DefaultSkipperClient.class);

	protected final RestTemplate restTemplate;

	private final String baseUri;

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
		ParameterizedTypeReference<Info> typeReference =
				new ParameterizedTypeReference<Info>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);

		ResponseEntity<Info> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/status/{releaseName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody();
	}

	@Override
	public Map<String, Info> statuses(String... releaseNames) {
		ParameterizedTypeReference<Map<String, Info>> typeReference =
			new ParameterizedTypeReference<Map<String, Info>>() { };

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "/release/statuses");
		builder.queryParam("names", StringUtils.arrayToCommaDelimitedString(releaseNames));

		ResponseEntity<Map<String, Info>> responseEntity =
				restTemplate.exchange(builder.toUriString(),
						HttpMethod.GET,
						null,
						typeReference);
		return responseEntity.getBody();
	}

	@Override
	public Map<String, Map<String, DeploymentState>> states(String... releaseNames) {
		ParameterizedTypeReference<Map<String, Map<String, DeploymentState>>> typeReference =
				new ParameterizedTypeReference<Map<String, Map<String, DeploymentState>>>() { };

		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "/release/states");
		builder.queryParam("names", StringUtils.arrayToCommaDelimitedString(releaseNames));

		ResponseEntity<Map<String, Map<String, DeploymentState>>> responseEntity =
				restTemplate.exchange(builder.toUriString(),
						HttpMethod.GET,
						null,
						typeReference);
		return responseEntity.getBody();
	}

	@Override
	public Info status(String releaseName, int releaseVersion) {
		ParameterizedTypeReference<Info> typeReference =
				new ParameterizedTypeReference<Info>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("releaseVersion", Integer.toString(releaseVersion));
		ResponseEntity<Info> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/status/{releaseName}/{releaseVersion}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody();
	}

	@Override
	public LogInfo getLog(String releaseName) {
		ParameterizedTypeReference<LogInfo> typeReference =
				new ParameterizedTypeReference<LogInfo>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);

		ResponseEntity<LogInfo> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/logs/{releaseName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody();
	}

	@Override
	public LogInfo getLog(String releaseName, String appName) {
		ParameterizedTypeReference<LogInfo> typeReference =
				new ParameterizedTypeReference<LogInfo>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("appName", appName);

		ResponseEntity<LogInfo> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/logs/{releaseName}/{appName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody();
	}

	@Override
	public Release scale(String releaseName, ScaleRequest scaleRequest) {
		ParameterizedTypeReference<Release> typeReference =
				new ParameterizedTypeReference<Release>() { };

		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);

		HttpEntity<ScaleRequest> httpEntity = new HttpEntity<>(scaleRequest);
		ResponseEntity<Release> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/scale/{releaseName}",
						HttpMethod.POST,
						httpEntity,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody();
	}

	@Override
	public String getFromActuator(String releaseName, String appName, String appId, String endpoint) {

		Map<String, Object> uriVariables = new HashMap<>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("appName", appName);
		uriVariables.put("appId", appId);

		ResponseEntity<String> resourceResponseEntity =
				restTemplate.exchange(
						baseUri + "/release/actuator/{releaseName}/{appName}/{appId}?endpoint=" + endpoint,
						HttpMethod.GET,
						null,
						String.class,
						uriVariables);

		return resourceResponseEntity.getBody();
	}

	@Override
	public Object postToActuator(String releaseName, String appName, String appId, ActuatorPostRequest request) {
		Map<String, Object> uriVariables = new HashMap<>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("appName", appName);
		uriVariables.put("appId", appId);

		HttpEntity<ActuatorPostRequest> httpEntity = new HttpEntity<>(request);
		ResponseEntity<?> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/actuator/{releaseName}/{appName}/{appId}",
						HttpMethod.POST,
						httpEntity,
						Object.class,
						uriVariables);

		return resourceResponseEntity.getBody();
	}

	@Override
	public String manifest(String releaseName) {
		ParameterizedTypeReference<Manifest> typeReference =
				new ParameterizedTypeReference<Manifest>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		ResponseEntity<Manifest> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/manifest/{releaseName}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody().getData();
	}

	@Override
	public String manifest(String releaseName, int releaseVersion) {
		ParameterizedTypeReference<Manifest> typeReference =
				new ParameterizedTypeReference<Manifest>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("releaseName", releaseName);
		uriVariables.put("releaseVersion", Integer.toString(releaseVersion));
		ResponseEntity<Manifest> resourceResponseEntity =
				restTemplate.exchange(baseUri + "/release/manifest/{releaseName}/{releaseVersion}",
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		return resourceResponseEntity.getBody().getData();
	}

	@Override
	public Collection<PackageMetadata> search(String name, boolean details) {
		ParameterizedTypeReference<HateoasResponseWrapper<PackageMetadatasResponseWrapper>> typeReference =
				new ParameterizedTypeReference<HateoasResponseWrapper<PackageMetadatasResponseWrapper>>() { };
		Map<String, String> uriVariables = new HashMap<String, String>();
		uriVariables.put("size", "2000");
		String url = baseUri + "/packageMetadata";
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
		builder.queryParam("size", "2000");
		if (StringUtils.hasText(name)) {
			uriVariables.put("name", name);
			builder.pathSegment("search", "findByNameContainingIgnoreCase");
			builder.queryParam("name", name);
		}
		if (!details) {
			builder.queryParam("projection", "summary");
			builder.queryParam("sort", "name,asc");
		}

		ResponseEntity<HateoasResponseWrapper<PackageMetadatasResponseWrapper>> resourceResponseEntity =
				restTemplate.exchange(builder.toUriString(),
						HttpMethod.GET,
						null,
						typeReference,
						uriVariables);
		PackageMetadatasResponseWrapper embedded = resourceResponseEntity.getBody().getEmbedded();
		if (embedded != null) {
			return embedded.getPackageMetadata();
		}
		else {
			return Collections.emptyList();
		}
	}

	public Release install(InstallRequest installRequest) {
		ParameterizedTypeReference<Release> typeReference =
				new ParameterizedTypeReference<Release>() { };
		String url = String.format("%s/%s/%s", baseUri, "package", "install");

		HttpEntity<InstallRequest> httpEntity = new HttpEntity<>(installRequest);
		ResponseEntity<Release> resourceResponseEntity =
				restTemplate.exchange(url, HttpMethod.POST, httpEntity, typeReference);
		return resourceResponseEntity.getBody();
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
		ParameterizedTypeReference<Release> typeReference =
				new ParameterizedTypeReference<Release>() { };
		String url = String.format("%s/%s/%s", baseUri, "release", "rollback");

		HttpEntity<RollbackRequest> httpEntity = new HttpEntity<>(rollbackRequest);
		ResponseEntity<Release> resourceResponseEntity =
				restTemplate.exchange(url, HttpMethod.POST, httpEntity, typeReference);
		return resourceResponseEntity.getBody();
	}

	@Override
	public List<Release> list(String releaseNameLike) {
		ParameterizedTypeReference<HateoasResponseWrapper<ReleasesResponseWrapper>> typeReference =
			new ParameterizedTypeReference<HateoasResponseWrapper<ReleasesResponseWrapper>>() { };
		String url;
		if (StringUtils.hasText(releaseNameLike)) {
			url = String.format("%s/%s/%s/%s", baseUri, "release", "list", releaseNameLike);
		}
		else {
			url = String.format("%s/%s/%s", baseUri, "release", "list");
		}
		ReleasesResponseWrapper embedded = this.restTemplate
				.exchange(url, HttpMethod.GET, null, typeReference, new HashMap<>()).getBody().getEmbedded();
		if (embedded != null && embedded.getReleases() != null) {
			return embedded.getReleases().stream().collect(Collectors.toList());
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<Release> history(String releaseName) {
		ParameterizedTypeReference<HateoasResponseWrapper<ReleasesResponseWrapper>> typeReference =
			new ParameterizedTypeReference<HateoasResponseWrapper<ReleasesResponseWrapper>>() { };
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri);
		builder.pathSegment("releases", "search", "findByNameIgnoreCaseContainingOrderByNameAscVersionDesc");
		builder.queryParam("name", releaseName);

		ResponseEntity<HateoasResponseWrapper<ReleasesResponseWrapper>> resourceResponseEntity =
				restTemplate.exchange(builder.toUriString(),
						HttpMethod.GET,
						null,
						typeReference);
		ReleasesResponseWrapper embedded = resourceResponseEntity.getBody().getEmbedded();
		if (embedded != null) {
			return embedded.getReleases();
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<Repository> listRepositories() {
		ParameterizedTypeReference<HateoasResponseWrapper<RepositoriesResponseWrapper>> typeReference =
				new ParameterizedTypeReference<HateoasResponseWrapper<RepositoriesResponseWrapper>>() { };
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "/repositories");
		builder.queryParam("size", "2000");

		ResponseEntity<HateoasResponseWrapper<RepositoriesResponseWrapper>> resourceResponseEntity =
				restTemplate.exchange(builder.toUriString(),
						HttpMethod.GET,
						null,
						typeReference);
		RepositoriesResponseWrapper embedded = resourceResponseEntity.getBody().getEmbedded();
		if (embedded != null) {
			return embedded.getRepositories();
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public Collection<Deployer> listDeployers() {
		ParameterizedTypeReference<HateoasResponseWrapper<DeployersResponseWrapper>> typeReference =
				new ParameterizedTypeReference<HateoasResponseWrapper<DeployersResponseWrapper>>() { };
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUri + "/deployers");
		builder.queryParam("size", "2000");

		ResponseEntity<HateoasResponseWrapper<DeployersResponseWrapper>> resourceResponseEntity =
				restTemplate.exchange(builder.toUriString(),
						HttpMethod.GET,
						null,
						typeReference);
		DeployersResponseWrapper embedded = resourceResponseEntity.getBody().getEmbedded();
		if (embedded != null) {
			return embedded.getDeployers();
		}
		else {
			return Collections.emptyList();
		}
	}

	@Override
	public PackageMetadata upload(UploadRequest uploadRequest) {
		ParameterizedTypeReference<PackageMetadata> typeReference =
				new ParameterizedTypeReference<PackageMetadata>() { };
		String url = String.format("%s/%s/%s", baseUri, "package", "upload");
		log.debug("Uploading package {}-{} to repository {}.", uploadRequest.getName(), uploadRequest.getVersion(),
				uploadRequest.getRepoName());
		HttpEntity<UploadRequest> httpEntity = new HttpEntity<>(uploadRequest);
		ResponseEntity<PackageMetadata> resourceResponseEntity =
				restTemplate.exchange(url, HttpMethod.POST, httpEntity, typeReference);
		PackageMetadata packageMetadata = resourceResponseEntity.getBody();
		return packageMetadata;
	}

	@Override
	public void packageDelete(String packageName) {
		String url = String.format("%s/%s/%s", baseUri, "package", packageName);
		this.restTemplate.delete(url);
	}

	protected static class HateoasResponseWrapper<T> {
		private T embedded;

		public void setEmbedded(T embedded) {
			this.embedded = embedded;
		}

		@JsonProperty("_embedded")
		public T getEmbedded() {
			return embedded;
		}
	}

	protected static class RepositoriesResponseWrapper {
		private Collection<Repository> repositories;

		public void setRepositories(Collection<Repository> repositories) {
			this.repositories = repositories;
		}

		public Collection<Repository> getRepositories() {
			return repositories;
		}
	}

	protected static class DeployersResponseWrapper {
		private Collection<Deployer> deployers;

		public void setDeployers(Collection<Deployer> deployers) {
			this.deployers = deployers;
		}

		public Collection<Deployer> getDeployers() {
			return deployers;
		}
	}

	protected static class PackageMetadatasResponseWrapper {
		private Collection<PackageMetadata> packageMetadata;

		public void setPackageMetadata(Collection<PackageMetadata> packageMetadata) {
			this.packageMetadata = packageMetadata;
		}

		public Collection<PackageMetadata> getPackageMetadata() {
			return packageMetadata;
		}
	}

	protected static class ReleasesResponseWrapper {
		private Collection<Release> releases;

		public void setReleases(Collection<Release> releases) {
			this.releases = releases;
		}

		public Collection<Release> getReleases() {
			return releases;
		}
	}
}
