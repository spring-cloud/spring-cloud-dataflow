/*
 * Copyright 2015-2018 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.OperationNotSupportedException;

import org.springframework.cloud.dataflow.rest.UpdateStreamRequest;
import org.springframework.cloud.dataflow.rest.client.support.VersionUtils;
import org.springframework.cloud.dataflow.rest.resource.StreamAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.skipper.domain.Deployer;
import org.springframework.cloud.skipper.domain.PackageIdentifier;
import org.springframework.cloud.skipper.domain.Release;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link StreamOperations}.
 *
 * @author Ilayaperumal Gopinathan
 * @author Mark Fisher
 * @author Eric Bottard
 */
public class StreamTemplate implements StreamOperations {

	public static final String DEFINITIONS_REL = "streams/definitions";

	private static final String DEFINITION_REL = "streams/definitions/definition";

	private static final String DEPLOYMENTS_REL = "streams/deployments";

	private static final String DEPLOYMENT_REL = "streams/deployments/deployment";

	private static final String VALIDATION_REL = "streams/validation";

	private static final String LOGS_REL = "streams/logs/{streamName}";

	private static final String LOGS_APP_REL = "streams/logs/{streamName}/{appName}";


	private static final String VALIDATION_RELATION_VERSION = "1.7.0";

	private final RestTemplate restTemplate;

	private final Link definitionsLink;

	private final Link definitionLink;

	private final Link deploymentsLink;

	private final Link deploymentLink;

	private final Link validationLink;

	private final Link logsLink;

	private final Link logsAppLink;

	private final String dataFlowServerVersion;

	StreamTemplate(RestTemplate restTemplate, RepresentationModel<?> resources, String dataFlowServerVersion) {
		Assert.notNull(resources, "URI CollectionModel can't be null");
		Assert.notNull(resources.getLink(DEFINITIONS_REL), "Definitions relation is required");
		Assert.notNull(resources.getLink(DEFINITION_REL), "Definition relation is required");
		Assert.notNull(resources.getLink(DEPLOYMENTS_REL), "Deployments relation is required");
		Assert.notNull(resources.getLink(DEPLOYMENT_REL), "Deployment relation is required");
		Assert.notNull(resources.getLink(LOGS_REL), "Logs relation is required");
		Assert.notNull(resources.getLink(LOGS_APP_REL), "Logs app relation is required");

		if (VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(
				VersionUtils.getThreePartVersion(dataFlowServerVersion),
				VALIDATION_RELATION_VERSION)) {
			Assert.notNull(resources.getLink(VALIDATION_REL), "Validation relation for streams is required");
		}

		this.dataFlowServerVersion = dataFlowServerVersion;
		this.restTemplate = restTemplate;
		this.definitionsLink = resources.getLink(DEFINITIONS_REL).get();
		this.deploymentsLink = resources.getLink(DEPLOYMENTS_REL).get();
		this.definitionLink = resources.getLink(DEFINITION_REL).get();
		this.deploymentLink = resources.getLink(DEPLOYMENT_REL).get();
		this.validationLink = resources.getLink(VALIDATION_REL).get();
		this.logsLink = resources.getLink(LOGS_REL).get();
		this.logsAppLink = resources.getLink(LOGS_APP_REL).get();

	}

	@Override
	public StreamDefinitionResource.Page list() {
		String uriTemplate = definitionsLink.expand().getHref();
		uriTemplate = uriTemplate + "?size=2000";
		return restTemplate.getForObject(uriTemplate, StreamDefinitionResource.Page.class);
	}

	@Override
	public StreamDeploymentResource info(String name) {
		String uriTemplate = deploymentLink.expand(name).getHref();
		return restTemplate.getForObject(uriTemplate, StreamDeploymentResource.class);
	}

	@Override
	public StreamDefinitionResource createStream(String name, String definition, String description, boolean deploy) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("description", description);
		values.add("deploy", Boolean.toString(deploy));
		StreamDefinitionResource stream = restTemplate.postForObject(definitionsLink.expand().getHref(), values,
				StreamDefinitionResource.class);
		return stream;
	}

	@Override
	public void deploy(String name, Map<String, String> properties) {
		restTemplate.postForObject(deploymentLink.expand(name).getHref(), properties, Object.class);
	}

	@Override
	public void undeploy(String name) {
		restTemplate.delete(deploymentLink.expand(name).getHref());
	}

	@Override
	public void undeployAll() {
		restTemplate.delete(deploymentsLink.getHref());
	}

	@Override
	public void destroy(String name) {
		restTemplate.delete(definitionLink.expand(name).getHref());
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(definitionsLink.getHref());
	}

	@Override
	public void scaleApplicationInstances(String streamName,
			String appName, Integer count, Map<String, String> properties) {
		String url = String.format("%s/scale/%s/%s/instances/%s", deploymentsLink.getHref(), streamName, appName, count);
		restTemplate.postForObject(url, properties, Object.class);
	}

	@Override
	public String streamExecutionLog(String streamName) {
		return restTemplate.getForObject(logsLink.expand(streamName).getHref(), String.class);
	}

	@Override
	public String streamExecutionLog(String streamName, String appName) {
		return restTemplate.getForObject(logsAppLink.expand(streamName,appName).getHref(), String.class);
	}

	@Override
	public void updateStream(String streamName, String releaseName, PackageIdentifier packageIdentifier,
			Map<String, String> updateProperties, boolean force, List<String> appNames) {
		Assert.hasText(streamName, "Stream name cannot be null or empty");
		Assert.notNull(packageIdentifier, "PackageIdentifier cannot be null");
		Assert.hasText(packageIdentifier.getPackageName(), "Package Name cannot be null or empty");
		Assert.hasText(releaseName, "Release name cannot be null or empty");
		Assert.notNull(updateProperties, "UpdateProperties cannot be null");
		UpdateStreamRequest updateStreamRequest = new UpdateStreamRequest(releaseName, packageIdentifier,
				updateProperties, force, appNames);
		String url = deploymentsLink.getHref() + "/update/" + streamName;
		restTemplate.postForObject(url, updateStreamRequest, Object.class);
	}

	@Override
	public void rollbackStream(String streamName, int version) {
		Assert.hasText(streamName, "Release name cannot be null or empty");
		String url = deploymentsLink.getHref() + "/rollback/" + streamName + "/" + version;
		restTemplate.postForObject(url, null, Object.class);
	}

	@Override
	public String getManifest(String streamName, int version) {
		Assert.hasText(streamName, "Release name cannot be null or empty");
		String url = String.format("%s/%s/%s/%s", deploymentsLink.getHref(), "manifest", streamName, version);
		String manifest = restTemplate.getForObject(url, String.class);
		// TODO - DataFlow only uses Jackson Marshaller, which does strange things to Strings as
		// return values.
		// \n is converted to two ascii characters 92 and 110...
		String prunedManifest = manifest.substring(1, manifest.length() - 1);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < prunedManifest.length(); i++) {
			char testChar = prunedManifest.charAt(i);
			if ((int) testChar == 92) {
				if (i != prunedManifest.length() - 1) {
					char nChar = prunedManifest.charAt(i + 1);
					if ((int) nChar == 110) {
						sb.append("\n");
						i++;
					}
				}
			}
			else {
				sb.append(testChar);
			}
		}
		return sb.toString();
	}

	@Override
	public Collection<Release> history(String streamName) {
		Assert.hasText(streamName, "Release name cannot be null or empty");
		ParameterizedTypeReference<Collection<Release>> typeReference = new ParameterizedTypeReference<Collection<Release>>() {
		};
		Map<String, Object> parameters = new HashMap<>();
		String url = String.format("%s/%s/%s", deploymentsLink.getHref(), "history", streamName);
		return this.restTemplate.exchange(url, HttpMethod.GET, null, typeReference, parameters).getBody();
	}

	@Override
	public Collection<Deployer> listPlatforms() {
		ParameterizedTypeReference<Collection<Deployer>> typeReference = new ParameterizedTypeReference<Collection<Deployer>>() {
		};
		Map<String, Object> parameters = new HashMap<>();
		String url = deploymentsLink.getHref() + "/platform/list";
		return this.restTemplate.exchange(url, HttpMethod.GET, null, typeReference, parameters).getBody();
	}

	@Override
	public StreamDefinitionResource getStreamDefinition(String streamName) {
		String uriTemplate = this.definitionLink.expand(streamName).getHref();
		return restTemplate.getForObject(uriTemplate, StreamDefinitionResource.class);
	}

	@Override
	public StreamAppStatusResource validateStreamDefinition(String streamDefinitionName)
			throws OperationNotSupportedException {
		if (validationLink == null) {
			throw new OperationNotSupportedException("Stream Validation not supported on Data Flow Server version "
					+ dataFlowServerVersion);
		}
		String uriTemplate = this.validationLink.expand(streamDefinitionName).getHref();
		return restTemplate.getForObject(uriTemplate, StreamAppStatusResource.class);
	}
}
