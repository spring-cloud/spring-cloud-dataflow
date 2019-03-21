/*
 * Copyright 2015-2017 the original author or authors.
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

package org.springframework.cloud.dataflow.server.controller;

import static org.springframework.hateoas.mvc.ControllerLinkBuilder.linkTo;

import org.springframework.analytics.rest.domain.AggregateCounterResource;
import org.springframework.analytics.rest.domain.CounterResource;
import org.springframework.analytics.rest.domain.FieldValueCounterResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionProgressInfoResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDeploymentResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
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
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
@RestController
@EnableConfigurationProperties(FeaturesProperties.class)
public class RootController {

	@Autowired
	private FeaturesProperties featuresProperties;

	/**
	 * Contains links pointing to controllers backing an entity type
	 * (such as streams).
	 */
	private final EntityLinks entityLinks;

	/**
	 * Construct an {@code RootController}.
	 *
	 * @param entityLinks holder of links to controllers and their associated entity types
	 */
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

		resourceSupport.add(linkTo(UiController.class).withRel("dashboard"));
		if (featuresProperties.isStreamsEnabled()) {
			resourceSupport.add(entityLinks.linkToCollectionResource(StreamDefinitionResource.class).withRel("streams/definitions"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(StreamDefinitionResource.class, "{name}").withRel("streams/definitions/definition")));
			resourceSupport.add(entityLinks.linkToCollectionResource(StreamDeploymentResource.class).withRel("streams/deployments"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(StreamDeploymentResource.class, "{name}").withRel("streams/deployments/deployment")));
			resourceSupport.add(entityLinks.linkToCollectionResource(AppStatusResource.class).withRel("runtime/apps"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkForSingleResource(AppStatusResource.class, "{appId}").withRel("runtime/apps/app")));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkFor(AppInstanceStatusResource.class, UriComponents.UriTemplateVariables.SKIP_VALUE).withRel("runtime/apps/instances")));
		}
		if (featuresProperties.isTasksEnabled()) {
			resourceSupport.add(entityLinks.linkToCollectionResource(TaskDefinitionResource.class).withRel("tasks/definitions"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskDefinitionResource.class, "{name}").withRel("tasks/definitions/definition")));
			resourceSupport.add(entityLinks.linkToCollectionResource(TaskDeploymentResource.class).withRel("tasks/deployments"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskDeploymentResource.class, "{name}").withRel("tasks/deployments/deployment")));
			resourceSupport.add(entityLinks.linkToCollectionResource(TaskExecutionResource.class).withRel("tasks/executions"));
			String taskTemplated = entityLinks.linkToCollectionResource(TaskExecutionResource.class).getHref() + "{?name}";
			resourceSupport.add(new Link(taskTemplated).withRel("tasks/executions/name"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskExecutionResource.class, "{id}").withRel("tasks/executions/execution")));
			resourceSupport.add(entityLinks.linkToCollectionResource(JobExecutionResource.class).withRel("jobs/executions"));
			taskTemplated = entityLinks.linkToCollectionResource(JobExecutionResource.class).getHref() + "{?name}";
			resourceSupport.add(new Link(taskTemplated).withRel("jobs/executions/name"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(JobExecutionResource.class, "{id}").withRel("jobs/executions/execution")));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkFor(StepExecutionResource.class, "{jobExecutionId}").withRel("jobs/executions/execution/steps")));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkFor(StepExecutionResource.class, "{jobExecutionId}").slash("{stepId}").withRel("jobs/executions/execution/steps/step")));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkFor(StepExecutionProgressInfoResource.class, "{jobExecutionId}").slash("{stepId}").slash("progress").withRel("jobs/executions/execution/steps/step/progress")));
			taskTemplated = entityLinks.linkToCollectionResource(JobInstanceResource.class).getHref() + "{?name}";
			resourceSupport.add(new Link(taskTemplated).withRel("jobs/instances/name"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(JobInstanceResource.class, "{id}").withRel("jobs/instances/instance")));
		}
		if (featuresProperties.isAnalyticsEnabled()) {
			resourceSupport.add(entityLinks.linkToCollectionResource(CounterResource.class).withRel("counters"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(CounterResource.class, "{name}").withRel("counters/counter")));
			resourceSupport.add(entityLinks.linkToCollectionResource(FieldValueCounterResource.class).withRel("field-value-counters"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(FieldValueCounterResource.class, "{name}").withRel("field-value-counters/counter")));
			resourceSupport.add(
					entityLinks.linkToCollectionResource(AggregateCounterResource.class).withRel("aggregate-counters"));
			resourceSupport.add(unescapeTemplateVariables(entityLinks
					.linkToSingleResource(AggregateCounterResource.class, "{name}").withRel("aggregate-counters/counter")));
		}
		resourceSupport.add(entityLinks.linkToCollectionResource(AppRegistrationResource.class).withRel("apps"));
		String completionStreamTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel().getHref() + ("/stream{?start,detailLevel}");
		resourceSupport.add(new Link(completionStreamTemplated).withRel("completions/stream"));
		String completionTaskTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel().getHref() + ("/task{?start,detailLevel}");
		resourceSupport.add(new Link(completionTaskTemplated).withRel("completions/task"));
		return resourceSupport;
	}

	// Workaround https://github.com/spring-projects/spring-hateoas/issues/234
	private Link unescapeTemplateVariables(Link raw) {
		return new Link(raw.getHref().replace("%7B", "{").replace("%7D", "}"), raw.getRel());
	}

}
