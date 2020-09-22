/*
 * Copyright 2015-2019 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.client.support.VersionUtils;
import org.springframework.cloud.dataflow.rest.resource.CurrentTaskExecutionsResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
import org.springframework.cloud.dataflow.rest.resource.TaskAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
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
 */
public class TaskTemplate implements TaskOperations {

	/* default */ static final String DEFINITIONS_RELATION = "tasks/definitions";

	private static final String DEFINITION_RELATION = "tasks/definitions/definition";

	private static final String EXECUTIONS_CURRENT_RELATION_VERSION = "1.7.0";

	private static final String VALIDATION_RELATION_VERSION = "1.7.0";

	private static final String EXECUTIONS_RELATION = "tasks/executions";

	private static final String EXECUTIONS_CURRENT_RELATION = "tasks/executions/current";

	private static final String EXECUTION_RELATION = "tasks/executions/execution";

	private static final String EXECUTION_RELATION_BY_NAME = "tasks/executions/name";

	private static final String VALIDATION_REL = "tasks/validation";

	private static final String PLATFORM_LIST_RELATION = "tasks/platforms";

	private static final String RETRIEVE_LOG = "tasks/logs";

	private final RestTemplate restTemplate;

	private final Link definitionsLink;

	private final Link definitionLink;

	private final Link executionsLink;

	private final Link executionLink;

	private final Link executionByNameLink;

	private final Link executionsCurrentLink;

	private final Link validationLink;

	private final Link platformListLink;

	private final String dataFlowServerVersion;

	private final Link retrieveLogLink;

	TaskTemplate(RestTemplate restTemplate, RepresentationModel<?> resources, String dataFlowServerVersion) {
		Assert.notNull(resources, "URI CollectionModel must not be be null");
		Assert.notNull(resources.getLink(EXECUTIONS_RELATION), "Executions relation is required");
		Assert.notNull(resources.getLink(DEFINITIONS_RELATION), "Definitions relation is required");
		Assert.notNull(resources.getLink(DEFINITION_RELATION), "Definition relation is required");
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		Assert.notNull(resources.getLink(EXECUTIONS_RELATION), "Executions relation is required");
		Assert.notNull(resources.getLink(EXECUTION_RELATION), "Execution relation is required");
		Assert.notNull(resources.getLink(EXECUTION_RELATION_BY_NAME), "Execution by name relation is required");
		Assert.notNull(dataFlowServerVersion, "dataFlowVersion must not be null");
		Assert.notNull(resources.getLink(RETRIEVE_LOG), "Log relation is required");

		this.dataFlowServerVersion = dataFlowServerVersion;

		if (VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(
				VersionUtils.getThreePartVersion(dataFlowServerVersion),
				VALIDATION_RELATION_VERSION)) {
			Assert.notNull(resources.getLink(VALIDATION_REL), "Validiation relation for tasks is required");
		}

		if (VersionUtils.isDataFlowServerVersionGreaterThanOrEqualToRequiredVersion(
				VersionUtils.getThreePartVersion(dataFlowServerVersion),
				EXECUTIONS_CURRENT_RELATION_VERSION)) {
			Assert.notNull(resources.getLink(EXECUTIONS_CURRENT_RELATION), "Executions current relation is required");
		}

		this.restTemplate = restTemplate;
		this.definitionsLink = resources.getLink(DEFINITIONS_RELATION).get();
		this.definitionLink = resources.getLink(DEFINITION_RELATION).get();
		this.executionsLink = resources.getLink(EXECUTIONS_RELATION).get();
		this.executionLink = resources.getLink(EXECUTION_RELATION).get();
		this.executionByNameLink = resources.getLink(EXECUTION_RELATION_BY_NAME).get();
		this.executionsCurrentLink = resources.getLink(EXECUTIONS_CURRENT_RELATION).get();
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
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		values.add("description", description);
        return restTemplate.postForObject(definitionsLink.expand().getHref(), values,
                TaskDefinitionResource.class);
	}

	@Override
	public long launch(String name, Map<String, String> properties, List<String> arguments, String alternateComposedTaskRunnerApp) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("properties", DeploymentPropertiesUtils.format(properties));
		values.add("arguments", StringUtils.collectionToDelimitedString(arguments, " "));
		if(alternateComposedTaskRunnerApp != null) {
			values.add("ctrname", alternateComposedTaskRunnerApp);
		}
		return restTemplate.postForObject(executionByNameLink.expand(name).getHref(), values, Long.class, name);
	}

	@Override
	public void stop(String ids) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		restTemplate.postForLocation(executionLink.expand(ids).getHref(),values);
	}

	@Override
	public void stop(String ids, String platform) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		values.add("platform", platform);
		restTemplate.postForLocation(executionLink.expand(ids).getHref(),values);
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
	public TaskExecutionResource.Page executionListByTaskName(String taskName) {
		return restTemplate.getForObject(executionByNameLink.expand(taskName).getHref(),
				TaskExecutionResource.Page.class);
	}

	@Override
	public TaskExecutionResource taskExecutionStatus(long id) {
		return restTemplate.getForObject(executionLink.expand(id).getHref(), TaskExecutionResource.class);
	}

	@Override
	public String taskExecutionLog(String externalExecutionId) {
		return taskExecutionLog(externalExecutionId, "default");
	}

	@Override
	public String taskExecutionLog(String externalExecutionId, String platform) {
		Map<String,String> map = new HashMap<>();
		map.put("taskExternalExecutionId",externalExecutionId);
		map.put("platformName", platform);
		return restTemplate.getForObject(retrieveLogLink.expand(map).getHref(), String.class);
	}

	@Override
	public Collection<CurrentTaskExecutionsResource> currentTaskExecutions() {
		ParameterizedTypeReference<Collection<CurrentTaskExecutionsResource>> typeReference =
			new ParameterizedTypeReference<Collection<CurrentTaskExecutionsResource>>() {
		};
		return restTemplate
			.exchange(executionsCurrentLink.getHref(),HttpMethod.GET,null, typeReference).getBody();
	}

	@Override
	public void cleanup(long id) {
		cleanup(id, false);
	}

	@Override
	public void cleanup(long id, boolean removeData) {
	    String uriTemplate = executionLink.expand(id).getHref();
	    if (removeData) {
	      uriTemplate = uriTemplate + "?action=CLEANUP,REMOVE_DATA";
	    }
	    restTemplate.delete(uriTemplate);
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
