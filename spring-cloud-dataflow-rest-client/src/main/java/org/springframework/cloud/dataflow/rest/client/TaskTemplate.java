/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.dataflow.rest.client;

import java.util.Collections;
import java.util.Map;

import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.util.DeploymentPropertiesUtils;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link org.springframework.cloud.dataflow.rest.client.TaskOperations}.
 *
 * @author Glenn Renfro
 * @author Michael Minella
 */
public class TaskTemplate implements TaskOperations {

	private static final String DEFINITIONS_RELATION = "tasks/definitions";

	private static final String DEPLOYMENTS_RELATION = "tasks/deployments";

	private static final String EXECUTIONS_RELATION = "tasks/executions";

	private final RestTemplate restTemplate;

	private final Link definitionsPath;

	private final Link deploymentsPath;

	private final Link executionsPath;

	TaskTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		Assert.notNull(resources, "URI Resources must not be be null");
		Assert.notNull(resources.getLink(EXECUTIONS_RELATION), "Executions path is required");
		Assert.notNull(resources.getLink(DEFINITIONS_RELATION), "Definitions path is required");
		Assert.notNull(resources.getLink(DEPLOYMENTS_RELATION), "Deployments path is required");
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		Assert.notNull(resources.getLink(EXECUTIONS_RELATION), "Executions path is required");

		this.restTemplate = restTemplate;
		this.definitionsPath = resources.getLink(DEFINITIONS_RELATION);
		this.deploymentsPath = resources.getLink(DEPLOYMENTS_RELATION);
		this.executionsPath = resources.getLink(EXECUTIONS_RELATION);

	}

	@Override
	public TaskDefinitionResource.Page list() {
		String uriTemplate = definitionsPath.getHref().toString();
		uriTemplate = uriTemplate + "?size=10000";
		return restTemplate.getForObject(uriTemplate, TaskDefinitionResource.Page.class);
	}

	@Override
	public TaskDefinitionResource create(String name, String definition) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("name", name);
		values.add("definition", definition);
		TaskDefinitionResource task = restTemplate.postForObject(
				definitionsPath.expand().getHref(), values, TaskDefinitionResource.class);
		return task;
	}

	@Override
	public void launch(String name, Map<String, String> properties) {
		String uriTemplate = deploymentsPath.getHref().toString() + "/{name}";
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<String, Object>();
		values.add("properties", DeploymentPropertiesUtils.format(properties));
		restTemplate.postForObject(uriTemplate, values, Object.class, name);
	}

	@Override
	public void destroy(String name) {
		String uriTemplate = definitionsPath.getHref().toString() + "/{name}";
		restTemplate.delete(uriTemplate, Collections.singletonMap("name", name));
	}

	@Override
	public TaskExecutionResource.Page executionList() {
		return restTemplate.getForObject(executionsPath.getHref().toString(),
				TaskExecutionResource.Page.class);
	}

	@Override
	public TaskExecutionResource.Page executionListByTaskName(String taskName) {
		String uriTemplate = executionsPath.getHref().toString();
		uriTemplate = uriTemplate + "/name/{taskName}";
		return restTemplate.getForObject(uriTemplate, TaskExecutionResource.Page.class,
				Collections.singletonMap("taskName", taskName));
	}

	@Override
	public TaskExecutionResource view(long id) {
		String uriTemplate = executionsPath.getHref().toString();
		uriTemplate = uriTemplate + "/id/{id}";

		return restTemplate.getForObject(uriTemplate, TaskExecutionResource.class,
			Collections.singletonMap("id", id));
	}

}
