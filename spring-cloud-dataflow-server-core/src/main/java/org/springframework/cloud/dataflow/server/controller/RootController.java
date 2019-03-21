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

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.analytics.rest.domain.AggregateCounterResource;
import org.springframework.analytics.rest.domain.CounterResource;
import org.springframework.analytics.rest.domain.FieldValueCounterResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.rest.resource.AppInstanceStatusResource;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.AppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.RootResource;
import org.springframework.cloud.dataflow.rest.resource.ScheduleInfoResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionProgressInfoResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.StreamDeploymentResource;
import org.springframework.cloud.dataflow.rest.resource.TaskAppStatusResource;
import org.springframework.cloud.dataflow.rest.resource.TaskDefinitionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.TaskToolsResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.hateoas.EntityLinks;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.mvc.ControllerLinkBuilder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@ExposesResourceFor(RootResource.class)
public class RootController {

	/**
	 * Contains links pointing to controllers backing an entity type (such as streams).
	 */
	private final EntityLinks entityLinks;

	@Autowired
	private FeaturesProperties featuresProperties;

	/**
	 * Construct an {@code RootController}.
	 *
	 * @param entityLinks holder of links to controllers and their associated entity types
	 */
	public RootController(EntityLinks entityLinks) {
		this.entityLinks = entityLinks;
	}

	/**
	 * Return a {@link ResourceSupport} object containing the resources served by the Data
	 * Flow server.
	 *
	 * @return {@code ResourceSupport} object containing the Data Flow server's resources
	 */
	@RequestMapping("/")
	public RootResource info() {
		RootResource root = new RootResource(Version.REVISION);

		root.add(ControllerLinkBuilder.linkTo(UiController.class).withRel("dashboard"));
		root.add(ControllerLinkBuilder.linkTo(AuditRecordController.class).withRel("audit-records"));

		if (featuresProperties.isStreamsEnabled()) {
			root.add(entityLinks.linkToCollectionResource(StreamDefinitionResource.class)
					.withRel("streams/definitions"));
			root.add(
					unescapeTemplateVariables(entityLinks.linkToSingleResource(StreamDefinitionResource.class, "{name}")
							.withRel("streams/definitions/definition")));
			root.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(StreamAppStatusResource.class, "{name}")
					.withRel("streams/validation")));
			root.add(entityLinks.linkToCollectionResource(AppStatusResource.class).withRel("runtime/apps"));
			root.add(unescapeTemplateVariables(
					entityLinks.linkForSingleResource(AppStatusResource.class, "{appId}").withRel("runtime/apps/app")));
			root.add(unescapeTemplateVariables(
					entityLinks.linkFor(AppInstanceStatusResource.class, "{appId}")
							.withRel("runtime/apps/instances")));
			root.add(ControllerLinkBuilder.linkTo(MetricsController.class).withRel("metrics/streams"));

			if (featuresProperties.isSkipperEnabled()) {
				root.add(ControllerLinkBuilder.linkTo(SkipperStreamDeploymentController.class).withRel("streams/deployments"));
				root.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SkipperStreamDeploymentController.class).deploy(null, null)).withRel("streams/deployments/{name}"));
				root.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SkipperStreamDeploymentController.class).history(null)).withRel("streams/deployments/history/{name}"));
				root.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SkipperStreamDeploymentController.class).manifest(null, null)).withRel("streams/deployments/manifest/{name}/{version}"));
				root.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SkipperStreamDeploymentController.class).platformList()).withRel("streams/deployments/platform/list"));
				root.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SkipperStreamDeploymentController.class).rollback(null, null)).withRel("streams/deployments/rollback/{name}/{version}"));
				root.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(SkipperStreamDeploymentController.class).update(null, null)).withRel("streams/deployments/update/{name}"));
			}
			else {
				root.add(ControllerLinkBuilder.linkTo(StreamDeploymentController.class).withRel("streams/deployments"));

			}
			root.add(
					unescapeTemplateVariables(entityLinks.linkToSingleResource(StreamDeploymentResource.class, "{name}")
							.withRel("streams/deployments/deployment")));
		}
		if (featuresProperties.isTasksEnabled()) {
			root.add(entityLinks.linkToCollectionResource(TaskDefinitionResource.class).withRel("tasks/definitions"));
			root.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskDefinitionResource.class, "{name}")
					.withRel("tasks/definitions/definition")));
			root.add(entityLinks.linkToCollectionResource(TaskExecutionResource.class).withRel("tasks/executions"));
			String taskTemplated = entityLinks.linkToCollectionResource(TaskExecutionResource.class).getHref()
					+ "{?name}";
			root.add(new Link(taskTemplated).withRel("tasks/executions/name"));
			root.add(ControllerLinkBuilder.linkTo(ControllerLinkBuilder.methodOn(TaskExecutionController.class)
				.getCurrentTaskExecutionsInfo()).withRel("tasks/executions/current"));
			root.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskExecutionResource.class, "{id}")
					.withRel("tasks/executions/execution")));
			root.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(TaskAppStatusResource.class, "{name}")
					.withRel("tasks/validation")));

			if(featuresProperties.isSchedulesEnabled()) {
				root.add(entityLinks.linkToCollectionResource(ScheduleInfoResource.class).withRel("tasks/schedules"));
				String scheduleTemplated = entityLinks.linkToCollectionResource(ScheduleInfoResource.class).getHref()
						+ "/instances/{taskDefinitionName}";
				root.add(new Link(scheduleTemplated).withRel("tasks/schedules/instances"));
			}
			root.add(entityLinks.linkToCollectionResource(JobExecutionResource.class).withRel("jobs/executions"));
			taskTemplated = entityLinks.linkToCollectionResource(JobExecutionResource.class).getHref() + "{?name}";
			root.add(new Link(taskTemplated).withRel("jobs/executions/name"));
			root.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(JobExecutionResource.class, "{id}")
					.withRel("jobs/executions/execution")));
			root.add(unescapeTemplateVariables(entityLinks.linkFor(StepExecutionResource.class, "{jobExecutionId}")
					.withRel("jobs/executions/execution/steps")));
			root.add(unescapeTemplateVariables(entityLinks.linkFor(StepExecutionResource.class, "{jobExecutionId}")
					.slash("{stepId}").withRel("jobs/executions/execution/steps/step")));
			root.add(unescapeTemplateVariables(
					entityLinks.linkFor(StepExecutionProgressInfoResource.class, "{jobExecutionId}").slash("{stepId}")
							.slash("progress").withRel("jobs/executions/execution/steps/step/progress")));
			taskTemplated = entityLinks.linkToCollectionResource(JobInstanceResource.class).getHref() + "{?name}";
			root.add(new Link(taskTemplated).withRel("jobs/instances/name"));
			root.add(unescapeTemplateVariables(entityLinks.linkToSingleResource(JobInstanceResource.class, "{id}")
					.withRel("jobs/instances/instance")));
			root.add(entityLinks.linkFor(TaskToolsResource.class).withRel("tools/parseTaskTextToGraph"));
			root.add(entityLinks.linkFor(TaskToolsResource.class).withRel("tools/convertTaskGraphToText"));
		}
		if (featuresProperties.isAnalyticsEnabled()) {
			root.add(entityLinks.linkToCollectionResource(CounterResource.class).withRel("counters"));
			root.add(unescapeTemplateVariables(
					entityLinks.linkToSingleResource(CounterResource.class, "{name}").withRel("counters/counter")));
			root.add(entityLinks.linkToCollectionResource(FieldValueCounterResource.class)
					.withRel("field-value-counters"));
			root.add(unescapeTemplateVariables(
					entityLinks.linkToSingleResource(FieldValueCounterResource.class, "{name}")
							.withRel("field-value-counters/counter")));
			root.add(
					entityLinks.linkToCollectionResource(AggregateCounterResource.class).withRel("aggregate-counters"));
			root.add(
					unescapeTemplateVariables(entityLinks.linkToSingleResource(AggregateCounterResource.class, "{name}")
							.withRel("aggregate-counters/counter")));
		}
		root.add(entityLinks.linkToCollectionResource(AppRegistrationResource.class).withRel("apps"));
		root.add(entityLinks.linkToCollectionResource(AboutResource.class).withRel("about"));

		String completionStreamTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel()
				.getHref() + ("/stream{?start,detailLevel}");
		root.add(new Link(completionStreamTemplated).withRel("completions/stream"));
		String completionTaskTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel().getHref()
				+ ("/task{?start,detailLevel}");
		root.add(new Link(completionTaskTemplated).withRel("completions/task"));

		return root;
	}

	// Workaround https://github.com/spring-projects/spring-hateoas/issues/234
	private Link unescapeTemplateVariables(Link raw) {
		return new Link(raw.getHref().replace("%7B", "{").replace("%7D", "}"), raw.getRel());
	}

}
