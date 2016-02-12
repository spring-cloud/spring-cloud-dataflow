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

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.cloud.dataflow.rest.resource.CounterResource;
import org.springframework.cloud.dataflow.rest.resource.FieldValueCounterResource;
import org.springframework.cloud.dataflow.rest.resource.LibraryRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.ModuleStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDeploymentResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;

/**
 * Controller for the root resource of the Data Flow server.
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 */
@RestController
public class RootController {

	/**
	 * Contains links pointing to controllers backing an entity type
	 * (such as streams).
	 */
	private final EntityLinks entityLinks;

	/**
	 * Construct an {@code ServerController}.
	 *
	 * @param entityLinks holder of links to controllers and their associated entity types
	 */
	@Autowired
	public RootController(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	/**
	 * Return a {@link ResourceSupport} object containing the resources
	 * served by the Data Flow server.
	 *
	 * @return {@code ResourceSupport} object containing the Data Flow server's resources
	 */
	@RequestMapping("/")
	public ResourceSupport info() {
		ResourceSupport resourceSupport = new ResourceSupport();
		resourceSupport.add(entityLinks.linkToCollectionResource(StreamDefinitionResource.class).withRel("streams/definitions"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(StreamDefinitionResource.class, "{name}").withRel("streams/definitions/definition")));

		resourceSupport.add(entityLinks.linkToCollectionResource(StreamDeploymentResource.class).withRel("streams/deployments"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(StreamDeploymentResource.class, "{name}").withRel("streams/deployments/deployment")));

		resourceSupport.add(entityLinks.linkToCollectionResource(TaskDefinitionResource.class).withRel("tasks/definitions"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskDefinitionResource.class, "{name}").withRel("tasks/definitions/definition")));

		resourceSupport.add(entityLinks.linkToCollectionResource(TaskDeploymentResource.class).withRel("tasks/deployments"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskDeploymentResource.class, "{name}").withRel("tasks/deployments/deployment")));

		resourceSupport.add(entityLinks.linkToCollectionResource(TaskExecutionResource.class).withRel("tasks/executions"));
		String templated = entityLinks.linkToCollectionResource(TaskExecutionResource.class).getHref() + "{?name}";
		resourceSupport.add(new Link(templated).withRel("tasks/executions/name"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskExecutionResource.class, "{id}").withRel("tasks/executions/execution")));

		resourceSupport.add(entityLinks.linkToCollectionResource(CounterResource.class).withRel("counters"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(CounterResource.class, "{name}").withRel("counters/counter")));

		resourceSupport.add(entityLinks.linkToCollectionResource(FieldValueCounterResource.class).withRel("field-value-counters"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(FieldValueCounterResource.class, "{name}").withRel("field-value-counters/counter")));

		resourceSupport.add(entityLinks.linkToCollectionResource(ModuleRegistrationResource.class).withRel("modules"));

		resourceSupport.add(entityLinks.linkToCollectionResource(LibraryRegistrationResource.class).withRel("libraries"));

		resourceSupport.add(entityLinks.linkToCollectionResource(ModuleStatusResource.class).withRel("runtime/modules"));
		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkForSingleResource(ModuleStatusResource.class, "{moduleId}").withRel("runtime/modules/module")));

		resourceSupport.add(unescapeTemplateVariables(entityLinks.linkFor(ModuleInstanceStatusResource.class, UriComponents.UriTemplateVariables.SKIP_VALUE).withRel("runtime/modules/instances")));

		templated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel().getHref() + ("/stream{?start,detailLevel}");
		resourceSupport.add(new Link(templated).withRel("completions/stream"));

		return resourceSupport;
	}

	// Workaround https://github.com/spring-projects/spring-hateoas/issues/234
	private Link unescapeTemplateVariables(Link raw) {
		return new Link(raw.getHref().replace("%7B", "{").replace("%7D", "}"), raw.getRel());
	}

}
