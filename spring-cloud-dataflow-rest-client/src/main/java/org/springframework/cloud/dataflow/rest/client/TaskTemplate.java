/*
 * Copyright 2015-2016 the original author or authors.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link org.springframework.cloud.dataflow.rest.client.TaskOperations}.
 * @author Glenn Renfro
 * @author Michael Minella
 * @author Gunnar Hillert
 */
public class TaskTemplate implements TaskOperations {

	public static final String DEFINITIONS_RELATION = "tasks/definitions";

	private static final String DEFINITION_RELATION = "tasks/definitions/definition";

	private static final String DEPLOYMENTS_RELATION = "tasks/deployments";

	private static final String DEPLOYMENT_RELATION = "tasks/deployments/deployment";

	private static final String EXECUTIONS_RELATION = "tasks/executions";

	private static final String EXECUTION_RELATION = "tasks/executions/execution";

	private static final String EXECUTION_RELATION_BY_NAME = "tasks/executions/name";

	private final RestTemplate restTemplate;

	private final Link definitionsLink;

	private final Link definitionLink;

	private final Link deploymentsLink;

	private final Link deploymentLink;

	private final Link executionsLink;

	private final Link executionLink;

	private final Link executionByNameLink;

	TaskTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		Assert.notNull(resources, "URI Resources must not be be null");
		Assert.notNull(resources.getLink(EXECUTIONS_RELATION), "Executions relation is required");
		Assert.notNull(resources.getLink(DEFINITIONS_RELATION), "Definitions relation is required");
		Assert.notNull(resources.getLink(DEPLOYMENTS_RELATION), "Deployments relation is required");
		Assert.notNull(resources.getLink(DEFINITION_RELATION), "Definition relation is required");
		Assert.notNull(resources.getLink(DEPLOYMENT_RELATION), "Deployment relation is required");
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		Assert.notNull(resources.getLink(EXECUTIONS_RELATION), "Executions relation is required");
		Assert.notNull(resources.getLink(EXECUTION_RELATION), "Execution relation is required");
		Assert.notNull(resources.getLink(EXECUTION_RELATION_BY_NAME), "Execution by name relation is required");


		this.restTemplate = restTemplate;
		this.definitionsLink = resources.getLink(DEFINITIONS_RELATION);
		this.definitionLink = resources.getLink(DEFINITION_RELATION);
		this.deploymentsLink = resources.getLink(DEPLOYMENTS_RELATION);
		this.deploymentLink = resources.getLink(DEPLOYMENT_RELATION);
		this.executionsLink = resources.getLink(EXECUTIONS_RELATION);
		this.executionLink = resources.getLink(EXECUTION_RELATION);
		this.executionByNameLink = resources.getLink(EXECUTION_RELATION_BY_NAME);

	}

	@Override
	public TaskDefinitionResource.Page list() {
		String uriTemplate = definitionsLink.getHref().toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, TaskDefinitionResource.Page.class);
	}

	@Override
	public TaskDefinitionResource create(String name, String definition) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		TaskDefinitionResource task = restTemplate.postForObject(
				definitionsLink.expand().getHref(), values, TaskDefinitionResource.class);
		return task;
	}

	@Override
	public void launch(String name, Map<String, String> properties, List<String> arguments) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("properties", DeploymentPropertiesUtils.format(properties));
		values.add("arguments", StringUtils.collectionToDelimitedString(arguments, " "));
		restTemplate.postForObject(deploymentLink.expand(name).getHref(), values, Object.class, name);
	}

	@Override
	public void destroy(String name) {
		restTemplate.delete(definitionLink.expand(name).getHref(), Collections.singletonMap("name", name));
	}

	@Override
	public TaskExecutionResource.Page executionList() {
		return restTemplate.getForObject(executionsLink.getHref().toString(),
				TaskExecutionResource.Page.class);
	}

	@Override
	public TaskExecutionResource.Page executionListByTaskName(String taskName) {
		return restTemplate.getForObject(executionByNameLink.expand(taskName).getHref(), TaskExecutionResource.Page.class);
	}

	@Override
	public TaskExecutionResource display(long id) {
		return restTemplate.getForObject(executionLink.expand(id).getHref(),
				TaskExecutionResource.class);
	}

}
