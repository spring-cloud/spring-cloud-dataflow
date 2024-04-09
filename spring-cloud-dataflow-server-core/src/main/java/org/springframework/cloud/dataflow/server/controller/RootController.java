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

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.cloud.dataflow.rest.resource.AppRegistrationResource;
import org.springframework.cloud.dataflow.rest.resource.CompletionProposalsResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionThinResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.LauncherResource;
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
import org.springframework.cloud.dataflow.rest.resource.TaskExecutionThinResource;
import org.springframework.cloud.dataflow.rest.resource.TaskToolsResource;
import org.springframework.cloud.dataflow.rest.resource.about.AboutResource;
import org.springframework.cloud.dataflow.server.config.features.FeaturesProperties;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.EntityLinks;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller for the root resource of the Data Flow server.
 *
 * @author Patrick Peralta
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Corneil du Plessis
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
	 * Return a {@link RepresentationModel} object containing the resources served by the Data
	 * Flow server.
	 *
	 * @return {@code RepresentationModel} object containing the Data Flow server's resources
	 */
	@RequestMapping("/")
	public RootResource info() {
		RootResource root = new RootResource(Version.REVISION);

		root.add(linkTo(UiController.class).withRel("dashboard"));
		root.add(linkTo(AuditRecordController.class).withRel("audit-records"));

		root.add(linkTo(methodOn(SchemaController.class).getVersions()).withRel("schema/versions"));
		root.add(linkTo(methodOn(SchemaController.class).getTargets()).withRel("schema/targets"));

		if (featuresProperties.isStreamsEnabled()) {
			root.add(entityLinks.linkToCollectionResource(StreamDefinitionResource.class)
					.withRel("streams/definitions"));
			root.add(
					unescapeTemplateVariables(entityLinks.linkToItemResource(StreamDefinitionResource.class, "{name}")
							.withRel("streams/definitions/definition")));
			root.add(unescapeTemplateVariables(entityLinks.linkToItemResource(StreamAppStatusResource.class, "{name}")
					.withRel("streams/validation")));

			root.add(linkTo(methodOn(RuntimeStreamsController.class).status(null, null, null)).withRel("runtime/streams"));
			root.add(linkTo(methodOn(RuntimeStreamsController.class).streamStatus(null, null, null)).withRel("runtime/streams/{streamNames}"));

			root.add(linkTo(methodOn(RuntimeAppsController.class).list(null, null)).withRel("runtime/apps"));
			root.add(linkTo(methodOn(RuntimeAppsController.class).display(null)).withRel("runtime/apps/{appId}"));

			root.add(linkTo(methodOn(RuntimeAppInstanceController.class).list(null, null, null)).withRel("runtime/apps/{appId}/instances"));
			root.add(linkTo(methodOn(RuntimeAppInstanceController.class).display(null, null)).withRel("runtime/apps/{appId}/instances/{instanceId}"));

			root.add(linkTo(methodOn(RuntimeAppInstanceController.class)
					.getFromActuator(null, null, null)).withRel("runtime/apps/{appId}/instances/{instanceId}/actuator"));
			root.add(linkTo(methodOn(RuntimeAppInstanceController.class)
					.postToActuator(null, null, null)).withRel("runtime/apps/{appId}/instances/{instanceId}/actuator"));

			root.add(linkTo(methodOn(RuntimeAppInstanceController.class)
				.postToUrl(null,null, null, null)).withRel("runtime/apps/{appId}/instances/{instanceId}/post"));

			root.add(linkTo(StreamDeploymentController.class).withRel("streams/deployments"));
			root.add(linkTo(methodOn(StreamDeploymentController.class).info(null, false)).withRel("streams/deployments/{name}{?reuse-deployment-properties}"));
			root.add(linkTo(methodOn(StreamDeploymentController.class).deploy(null, null)).withRel("streams/deployments/{name}"));
			root.add(linkTo(methodOn(StreamDeploymentController.class).history(null)).withRel("streams/deployments/history/{name}"));
			root.add(linkTo(methodOn(StreamDeploymentController.class).manifest(null, null)).withRel("streams/deployments/manifest/{name}/{version}"));
			root.add(linkTo(methodOn(StreamDeploymentController.class).platformList()).withRel("streams/deployments/platform/list"));
			root.add(linkTo(methodOn(StreamDeploymentController.class).rollback(null, null)).withRel("streams/deployments/rollback/{name}/{version}"));
			root.add(linkTo(methodOn(StreamDeploymentController.class).update(null, null)).withRel("streams/deployments/update/{name}"));
			root.add(unescapeTemplateVariables(entityLinks.linkToItemResource(StreamDeploymentResource.class, "{name}").withRel("streams/deployments/deployment")));
			root.add(linkTo(methodOn(StreamDeploymentController.class).scaleApplicationInstances(null, null, null, null)).withRel("streams/deployments/scale/{streamName}/{appName}/instances/{count}"));
			root.add(linkTo(StreamLogsController.class).withRel("streams/logs"));
			root.add(linkTo(methodOn(StreamLogsController.class).getLog(null)).withRel("streams/logs/{streamName}"));
			root.add(linkTo(methodOn(StreamLogsController.class).getLog(null, null)).withRel("streams/logs/{streamName}/{appName}"));
		}

		if (featuresProperties.isTasksEnabled()) {

			root.add(entityLinks.linkToCollectionResource(LauncherResource.class).withRel("tasks/platforms"));

			root.add(entityLinks.linkToCollectionResource(TaskDefinitionResource.class).withRel("tasks/definitions"));
			root.add(unescapeTemplateVariables(entityLinks.linkToItemResource(TaskDefinitionResource.class, "{name}")
					.withRel("tasks/definitions/definition")));
			root.add(entityLinks.linkToCollectionResource(TaskExecutionResource.class).withRel("tasks/executions"));
			root.add(linkTo(methodOn(TaskExecutionController.class).viewByExternal(null,null)).withRel("tasks/executions/external"));
			root.add(linkTo(methodOn(TaskExecutionController.class).launchBoot3(null,null,null)).withRel("tasks/executions/launch"));
			String taskTemplated = entityLinks.linkToCollectionResource(TaskExecutionResource.class).getHref()
					+ "{?name}";
			root.add(Link.of(taskTemplated).withRel("tasks/executions/name"));
			root.add(linkTo(methodOn(TaskExecutionController.class)
					.getCurrentTaskExecutionsInfo()).withRel("tasks/executions/current"));
			root.add(unescapeTemplateVariables(linkTo(methodOn(TaskExecutionController.class).view(null,null)).withRel("tasks/executions/execution")));
			root.add(unescapeTemplateVariables(entityLinks.linkToItemResource(TaskAppStatusResource.class, "{name}")
					.withRel("tasks/validation")));
			root.add(linkTo(methodOn(TasksInfoController.class).getInfo(null, null, null)).withRel("tasks/info/executions"));
			root.add(linkTo(methodOn(TaskLogsController.class).getLog(null, null, null)).withRel("tasks/logs"));
			root.add(linkTo(methodOn(TaskExecutionThinController.class).listTasks(null, null)).withRel("tasks/thinexecutions"));
			if (featuresProperties.isSchedulesEnabled()) {
				root.add(entityLinks.linkToCollectionResource(ScheduleInfoResource.class).withRel("tasks/schedules"));
				String scheduleTemplated = entityLinks.linkToCollectionResource(ScheduleInfoResource.class).getHref()
						+ "/instances/{taskDefinitionName}";
				root.add(Link.of(scheduleTemplated).withRel("tasks/schedules/instances"));
			}
			root.add(entityLinks.linkToCollectionResource(JobExecutionResource.class).withRel("jobs/executions"));
			taskTemplated = entityLinks.linkToCollectionResource(JobExecutionResource.class).getHref() + "{?name}";
			root.add(Link.of(taskTemplated).withRel("jobs/executions/name"));
			taskTemplated = entityLinks.linkToCollectionResource(JobExecutionResource.class).getHref() + "{?status}";
			root.add(Link.of(taskTemplated).withRel("jobs/executions/status"));
			root.add(unescapeTemplateVariables(entityLinks.linkToItemResource(JobExecutionResource.class, "{id}")
					.withRel("jobs/executions/execution")));
			root.add(unescapeTemplateVariables(entityLinks.linkFor(StepExecutionResource.class, "{jobExecutionId}")
					.withRel("jobs/executions/execution/steps")));
			root.add(unescapeTemplateVariables(entityLinks.linkFor(StepExecutionResource.class, "{jobExecutionId}")
					.slash("{stepId}").withRel("jobs/executions/execution/steps/step")));
			root.add(unescapeTemplateVariables(
					entityLinks.linkFor(StepExecutionProgressInfoResource.class, "{jobExecutionId}").slash("{stepId}")
							.slash("progress").withRel("jobs/executions/execution/steps/step/progress")));
			taskTemplated = entityLinks.linkToCollectionResource(JobInstanceResource.class).getHref() + "{?name}";
			root.add(Link.of(taskTemplated).withRel("jobs/instances/name"));
			root.add(unescapeTemplateVariables(entityLinks.linkToItemResource(JobInstanceResource.class, "{id}")
					.withRel("jobs/instances/instance")));
			root.add(entityLinks.linkFor(TaskToolsResource.class).withRel("tools/parseTaskTextToGraph"));
			root.add(entityLinks.linkFor(TaskToolsResource.class).withRel("tools/convertTaskGraphToText"));
			root.add(entityLinks.linkToCollectionResource(JobExecutionThinResource.class).withRel("jobs/thinexecutions"));
			taskTemplated = entityLinks.linkToCollectionResource(JobExecutionThinResource.class).getHref() + "{?name}";
			root.add(Link.of(taskTemplated).withRel("jobs/thinexecutions/name"));
			taskTemplated = entityLinks.linkToCollectionResource(JobExecutionThinResource.class).getHref() + "{?jobInstanceId}";
			root.add(Link.of(taskTemplated).withRel("jobs/thinexecutions/jobInstanceId"));
			taskTemplated = entityLinks.linkToCollectionResource(JobExecutionThinResource.class).getHref() + "{?taskExecutionId}";
			root.add(Link.of(taskTemplated).withRel("jobs/thinexecutions/taskExecutionId"));

		}
		root.add(entityLinks.linkToCollectionResource(AppRegistrationResource.class).withRel("apps"));
		root.add(entityLinks.linkToCollectionResource(AboutResource.class).withRel("about"));

		String completionStreamTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel()
				.getHref() + ("/stream{?start,detailLevel}");
		root.add(Link.of(completionStreamTemplated).withRel("completions/stream"));
		String completionTaskTemplated = entityLinks.linkFor(CompletionProposalsResource.class).withSelfRel().getHref()
				+ ("/task{?start,detailLevel}");
		root.add(Link.of(completionTaskTemplated).withRel("completions/task"));

		return root;
	}

	// Workaround https://github.com/spring-projects/spring-hateoas/issues/234
	private Link unescapeTemplateVariables(Link raw) {
		return Link.of(raw.getHref().replace("%7B", "{").replace("%7D", "}"), raw.getRel());
	}

}
