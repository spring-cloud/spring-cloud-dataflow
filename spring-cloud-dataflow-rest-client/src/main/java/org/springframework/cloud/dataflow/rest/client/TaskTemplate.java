/*
 * Copyright 2015-2023 the original author or authors.
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
import java.util.Objects;
import java.util.stream.Stream;

import javax.naming.OperationNotSupportedException;

import org.springframework.cloud.dataflow.rest.client.support.VersionUtils;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LaunchResponseResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.rest.resource.TaskAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionThinResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionsInfoResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.http.HttpMethod;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for
 * {@link org.springframework.cloud.dataflow.rest.client.TaskOperations}.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Gunnar Hillert
 * @author David Turanski
 * @author Corneil du Plessis
 */
public class TaskTemplate implements TaskOperations {

	/* default */ static final String DEFINITIONS_RELATION = "tasks/definitions";

	private static final String DEFINITION_RELATION = "tasks/definitions/definition";

	private static final String EXECUTIONS_CURRENT_RELATION_VERSION = "1.7.0";

	private static final String VALIDATION_RELATION_VERSION = "1.7.0";
	private static final String VALIDATION_THIN_TASK_VERSION = "2.11.3";
	private static final String EXECUTIONS_RELATION = "tasks/executions";

	private static final String THIN_EXECUTIONS_RELATION = "tasks/thinexecutions";

	private static final String THIN_EXECUTIONS_BY_NAME_RELATION = "tasks/thinexecutions/name";

	private static final String EXECUTIONS_CURRENT_RELATION = "tasks/executions/current";

	private static final String EXECUTION_RELATION = "tasks/executions/execution";

	private static final String EXECUTION_LAUNCH_RELATION = "tasks/executions/launch";

	private static final String EXECUTION_RELATION_BY_NAME = "tasks/executions/name";

	private static final String EXECUTIONS_INFO_RELATION = "tasks/info/executions";

	private static final String VALIDATION_REL = "tasks/validation";

	private static final String PLATFORM_LIST_RELATION = "tasks/platforms";

	private static final String RETRIEVE_LOG = "tasks/logs";

	private final RestTemplate restTemplate;

	private final Link definitionsLink;

	private final Link definitionLink;

	private final Link executionsLink;

	private final Link thinExecutionsLink;

	private final Link thinExecutionsByNameLink;

	private final Link executionLink;

	private final Link executionLaunchLink;

	private final Link executionByNameLink;

	private final Link executionsCurrentLink;

	private Link executionsInfoLink;

	private final Link validationLink;

	private final Link platformListLink;

	private final String dataFlowServerVersion;
	private String actualDataFlowServerCoreVersion = null;

	private final Link retrieveLogLink;
	private final Link aboutLink;
	TaskTemplate(RestTemplate restTemplate, RepresentationModel<?> resources, String dataFlowServerVersion) {
		Assert.notNull(resources, "URI CollectionModel must not be be null");
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		Assert.notNull(dataFlowServerVersion, "dataFlowVersion must not be null");
		Assert.isTrue(resources.getLink("about").isPresent(), "Expected about relation");
		Stream.of(
			"about",
			DEFINITIONS_RELATION,
			DEFINITION_RELATION,
			EXECUTIONS_RELATION,
			EXECUTION_RELATION,
			EXECUTION_RELATION_BY_NAME,
			EXECUTIONS_INFO_RELATION,
			PLATFORM_LIST_RELATION,
			RETRIEVE_LOG,
			VALIDATION_REL
		).forEach(relation -> {
			Assert.isTrue(resources.getLink(relation).isPresent(), () -> relation + " relation is required");
		});
		this.dataFlowServerVersion = dataFlowServerVersion;
		this.restTemplate = restTemplate;

		String version = VersionUtils.getThreePartVersion(dataFlowServerVersion);
		if (VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(version, VALIDATION_RELATION_VERSION)) {
			Assert.notNull(resources.getLink(VALIDATION_REL), ()-> VALIDATION_REL + " relation is required");
		}

		this.aboutLink = resources.getLink("about").get();

		this.definitionsLink = resources.getLink(DEFINITIONS_RELATION).get();
		this.definitionLink = resources.getLink(DEFINITION_RELATION).get();
		this.executionsLink = resources.getLink(EXECUTIONS_RELATION).get();
		this.executionLink = resources.getLink(EXECUTION_RELATION).get();
		this.executionsCurrentLink = resources.getLink(EXECUTIONS_CURRENT_RELATION).orElse(null);
		this.thinExecutionsLink = resources.getLink(THIN_EXECUTIONS_RELATION).orElse(null);
		this.thinExecutionsByNameLink = resources.getLink(THIN_EXECUTIONS_BY_NAME_RELATION).orElse(null);
		this.executionLaunchLink = resources.getLink(EXECUTION_LAUNCH_RELATION).orElse(null);
		this.executionByNameLink = resources.getLink(EXECUTION_RELATION_BY_NAME).get();
		if (resources.getLink(EXECUTIONS_INFO_RELATION).isPresent()) {
			this.executionsInfoLink = resources.getLink(EXECUTIONS_INFO_RELATION).get();
		}
		this.validationLink = resources.getLink(VALIDATION_REL).get();
		this.platformListLink = resources.getLink(PLATFORM_LIST_RELATION).get();
		this.retrieveLogLink = resources.getLink(RETRIEVE_LOG).get();
	}

	@Override
	public TaskDefinitionResource.Page list() {
		String uriTemplate = definitionsLink.getHref();
		uriTemplate = uriTemplate + "?size=2000";
		return restTemplate.getForObject(uriTemplate, TaskDefinitionResource.Page.class);
	}

	@Override
	public LauncherResource.Page listPlatforms() {
		String uriTemplate = this.platformListLink.getHref();
		uriTemplate = uriTemplate + "?size=2000";
		return restTemplate.getForObject(uriTemplate, LauncherResource.Page.class);
	}

	@Override
	public TaskDefinitionResource create(String name, String definition, String description) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("description", description);
		return restTemplate.postForObject(definitionsLink.expand().getHref(), values,
			TaskDefinitionResource.class);
	}
	private boolean isNewServer() {
		if(this.actualDataFlowServerCoreVersion == null) {
			AboutResource aboutResource = restTemplate.getForObject(aboutLink.expand().getHref(), AboutResource.class);
			Assert.notNull(aboutResource, "Expected about");
			this.actualDataFlowServerCoreVersion = aboutResource.getVersionInfo().getCore().getVersion();
		}
		String v2_11_0 = VersionUtils.getThreePartVersion("2.11.0-SNAPSHOT");
		String serverVersion = VersionUtils.getThreePartVersion(this.actualDataFlowServerCoreVersion);
		return VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(serverVersion, v2_11_0);
	}
	@Override
	public LaunchResponseResource launch(String name, Map<String, String> properties, List<String> arguments) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		String formattedProperties = DeploymentPropertiesUtils.format(properties);
		String commandLineArguments = StringUtils.collectionToDelimitedString(arguments, " ");
		values.add("properties", formattedProperties);
		values.add("arguments", commandLineArguments);
		if(isNewServer()) {
			Assert.notNull(executionLaunchLink, "This version of SCDF doesn't support tasks/executions/launch");
			values.add("name", name);
			String url = executionLaunchLink.expand(name).getHref();
			values.remove("name");
			return restTemplate.postForObject(url, values, LaunchResponseResource.class);
		} else {
			Long id = restTemplate.postForObject(executionByNameLink.expand(name).getHref(), values, Long.class, name);
			if(id != null) {
				LaunchResponseResource response = new LaunchResponseResource();
				response.setExecutionId(id);
				return response;
			} else {
				throw new RuntimeException("Expected id");
			}
		}
	}

	@Override
	public void stop(String ids) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		restTemplate.postForLocation(executionLink.expand(ids).getHref(), values);
	}

	@Override
	public void stop(String ids, String platform) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("platform", platform);
		restTemplate.postForLocation(executionLink.expand(ids).getHref(), values);
	}

	@Override
	public void destroy(String name) {
		restTemplate.delete(definitionLink.expand(name).getHref());
	}

	@Override
	public void destroy(String name, boolean cleanup) {
		String url = (cleanup) ? definitionLink.expand(name).getHref() + "?cleanup=true" : definitionLink.expand(name).getHref();
		restTemplate.delete(url);
	}

	@Override
	public void destroyAll() {
		restTemplate.delete(definitionsLink.getHref());
	}

	@Override
	public TaskExecutionResource.Page executionList() {
		return restTemplate.getForObject(executionsLink.getHref(), TaskExecutionResource.Page.class);
	}

	@Override
	public PagedModel<TaskExecutionThinResource> thinExecutionList() {
		if(thinExecutionsLink != null) {
			return restTemplate.getForObject(thinExecutionsLink.getHref(), TaskExecutionThinResource.Page.class);
		} else {
			return restTemplate.getForObject(executionsLink.getHref(), TaskExecutionThinResource.Page.class);
		}
	}

	@Override
	public PagedModel<TaskExecutionThinResource> thinExecutionListByTaskName(String taskName) {
		if(thinExecutionsByNameLink != null) {
			return restTemplate.getForObject(thinExecutionsByNameLink.expand(taskName).getHref(), TaskExecutionThinResource.Page.class);
		} else {
			return restTemplate.getForObject(executionByNameLink.expand(taskName).getHref(), TaskExecutionThinResource.Page.class);
		}
	}

	@Override
	public TaskExecutionResource.Page executionListByTaskName(String taskName) {
		return restTemplate.getForObject(executionByNameLink.expand(taskName).getHref(),
			TaskExecutionResource.Page.class);
	}

	@Override
	public TaskExecutionResource taskExecutionStatus(long id) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("id", id);
		String url = executionLink.expand(values).getHref();
		return restTemplate.getForObject(url, TaskExecutionResource.class);
	}

	@Override
	public String taskExecutionLog(String externalExecutionId) {
		return taskExecutionLog(externalExecutionId, "default");
	}

	@Override
	public String taskExecutionLog(String externalExecutionId, String platform) {
		Map<String, String> map = new HashMap<>();
		map.put("taskExternalExecutionId", externalExecutionId);
		map.put("platformName", platform);
		return restTemplate.getForObject(retrieveLogLink.expand(map).getHref(), String.class);
	}

	@Override
	public Collection<CurrentTaskExecutionsResource> currentTaskExecutions() {
		ParameterizedTypeReference<Collection<CurrentTaskExecutionsResource>> typeReference =
			new ParameterizedTypeReference<Collection<CurrentTaskExecutionsResource>>() {
			};
		return restTemplate
			.exchange(executionsCurrentLink.getHref(), HttpMethod.GET, null, typeReference).getBody();
	}

	@Override
	public void cleanup(long id) {
		cleanup(id, false);
	}

	@Override
	public void cleanup(long id,  boolean removeData) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();

		String uriTemplate = executionLink.expand(id).getHref();

		if (removeData) {
			uriTemplate = uriTemplate + "?action=CLEANUP,REMOVE_DATA";
		}

		restTemplate.delete(uriTemplate);
	}

	@Override
	public void cleanupAllTaskExecutions(boolean completed, String taskName) {
		String uriTemplate = this.executionsLink.getHref() + "?action=CLEANUP,REMOVE_DATA";
		if (completed) {
			uriTemplate = uriTemplate + "&completed=true";
		}
		if (StringUtils.hasText(taskName)) {
			uriTemplate = uriTemplate + "&name=" + taskName;
		}
		this.restTemplate.delete(uriTemplate);
	}

	@Override
	public Integer getAllTaskExecutionsCount(boolean completed, String taskName) {
		Map<String, String> map = new HashMap<>();
		map.put("completed", String.valueOf(completed));
		map.put("name", StringUtils.hasText(taskName) ? taskName : "");
		if (this.executionsInfoLink != null) {
			return Objects.requireNonNull(
				restTemplate.getForObject(this.executionsInfoLink.expand(map).getHref(), TaskExecutionsInfoResource.class)
			).getTotalExecutions();
		}
		// for backwards-compatibility return zero count
		return 0;
	}


	@Override
	public TaskAppStatusResource validateTaskDefinition(String taskDefinitionName)
		throws OperationNotSupportedException {
		if (validationLink == null) {
			throw new OperationNotSupportedException("Task Validation not supported on Data Flow Server version "
				+ dataFlowServerVersion);
		}
		String uriTemplate = this.validationLink.expand(taskDefinitionName).getHref();
		return restTemplate.getForObject(uriTemplate, TaskAppStatusResource.class);
	}
}
