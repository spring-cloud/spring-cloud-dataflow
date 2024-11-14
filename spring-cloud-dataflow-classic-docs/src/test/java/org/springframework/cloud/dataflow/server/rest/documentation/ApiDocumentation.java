/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.cloud.dataflow.server.rest.documentation;

import javax.servlet.RequestDispatcher;

import org.junit.jupiter.api.Test;

import org.springframework.cloud.dataflow.rest.Version;
import org.springframework.restdocs.payload.JsonFieldType;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.responseHeaders;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Gunnar Hillert
 * @author Christian Tzolov
 * @author Ilayaperumal Gopinathan
 * @author Corneil du Plessis
 */
@SuppressWarnings("NewClassNamingConvention")
public class ApiDocumentation extends BaseDocumentation {

	@Test
	public void headers() throws Exception {
		this.mockMvc.perform(get("/")).andExpect(status().isOk())
				.andDo(this.documentationHandler.document(responseHeaders(headerWithName("Content-Type")
						.description("The Content-Type of the payload, e.g. " + "`application/hal+json`"))));
	}

	@Test
	public void errors() throws Exception {
		this.mockMvc
				.perform(get("/error").requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
						.requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/apps").requestAttr(
								RequestDispatcher.ERROR_MESSAGE,
								"The app 'http://localhost:8080/apps/123' does " + "not exist"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("error", is("Bad Request")))
				.andExpect(jsonPath("timestamp", is(notNullValue()))).andExpect(jsonPath("status", is(400)))
				.andExpect(jsonPath("path", is(notNullValue())))
				.andDo(this.documentationHandler.document(responseFields(
						fieldWithPath("error").description(
								"The HTTP error that occurred, e.g. `Bad Request`"),
						fieldWithPath("message").description("A description of the cause of the error"),
						fieldWithPath("path").description("The path to which the request was made"),
						fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
						fieldWithPath("timestamp")
								.description("The time, in milliseconds, at which the error" + " occurred"))));
	}

	@Test
	public void index() throws Exception {
		this.mockMvc.perform(get("/"))
				.andExpect(status().isOk())
				.andDo(this.documentationHandler.document(links(
				linkWithRel("about").description(
						"Access meta information, including enabled " + "features, security info, version information"),

				linkWithRel("dashboard").description("Access the dashboard UI"),
				linkWithRel("audit-records").description("Provides audit trail information"),
				linkWithRel("apps").description("Handle registered applications"),
				linkWithRel("completions/stream").description("Exposes the DSL completion features " + "for Stream"),
				linkWithRel("completions/task").description("Exposes the DSL completion features for " + "Task"),

				linkWithRel("jobs/executions").description("Provides the JobExecution resource"),
				linkWithRel("jobs/thinexecutions").description("Provides the JobExecution thin resource with no step executions included"),
				linkWithRel("jobs/executions/execution").description("Provides details for a specific JobExecution"),
				linkWithRel("jobs/executions/execution/steps").description("Provides the steps for a JobExecution"),
				linkWithRel("jobs/executions/execution/steps/step").description("Returns the details for a specific step"),
				linkWithRel("jobs/executions/execution/steps/step/progress").description("Provides progress information for a specific step"),
				linkWithRel("jobs/executions/name").description("Retrieve Job Executions by Job name"),
				linkWithRel("jobs/executions/status").description("Retrieve Job Executions by Job status"),
				linkWithRel("jobs/thinexecutions/name").description("Retrieve Job Executions by Job name with no step executions included"),
				linkWithRel("jobs/thinexecutions/jobInstanceId").description("Retrieve Job Executions by Job Instance Id with no step executions included"),
				linkWithRel("jobs/thinexecutions/taskExecutionId").description("Retrieve Job Executions by Task Execution Id with no step executions included"),
				linkWithRel("jobs/instances/instance").description("Provides the job instance resource for a specific job instance"),
				linkWithRel("jobs/instances/name").description("Provides the Job instance resource for a specific job name"),

				linkWithRel("runtime/streams").description("Exposes stream runtime status"),
				linkWithRel("runtime/streams/{streamNames}").description("Exposes streams runtime status for a given stream names"),
				linkWithRel("runtime/apps").description("Provides the runtime application resource"),
				linkWithRel("runtime/apps/{appId}").description("Exposes the runtime status for a specific app"),
				linkWithRel("runtime/apps/{appId}/instances").description("Provides the status for app instances"),
				linkWithRel("runtime/apps/{appId}/instances/{instanceId}").description("Provides the status for specific app instance"),
				linkWithRel("runtime/apps/{appId}/instances/{instanceId}/actuator").description("EXPERIMENTAL: Allows invoking Actuator endpoint on specific app instance"),
				linkWithRel("runtime/apps/{appId}/instances/{instanceId}/post").description("EXPERIMENTAL: Allows POST on http sink"),

				linkWithRel("tasks/definitions").description("Provides the task definition resource"),
				linkWithRel("tasks/definitions/definition").description("Provides details for a specific task definition"),
				linkWithRel("tasks/validation").description("Provides the validation for a task definition"),
				linkWithRel("tasks/executions").description("Returns Task executions"),
				linkWithRel("tasks/executions/launch").description("Provides for launching a Task execution"),
				linkWithRel("tasks/executions/external").description("Returns Task execution by external id"),
				linkWithRel("tasks/executions/current").description("Provides the current count of running tasks"),
				linkWithRel("tasks/info/executions").description("Provides the task executions info"),
				linkWithRel("tasks/schedules").description("Provides schedule information of tasks"),
				linkWithRel("tasks/schedules/instances").description("Provides schedule information of a specific task	"),
				linkWithRel("tasks/executions/name").description("Returns all task executions for a given Task name"),
				linkWithRel("tasks/executions/execution").description("Provides details for a specific task execution"),
				linkWithRel("tasks/platforms").description("Provides platform accounts for launching tasks.  The results can be filtered to show the platforms that support scheduling by adding a request parameter of 'schedulesEnabled=true"),
				linkWithRel("tasks/logs").description("Retrieve the task application log"),
				linkWithRel("tasks/thinexecutions").description("Returns thin Task executions"),
				linkWithRel("tasks/thinexecutions/name").description("Returns all thin Task executions for a given Task name"),

				linkWithRel("schema/versions").description("List of Spring Boot related schemas"),
				linkWithRel("schema/targets").description("List of schema targets"),

				linkWithRel("streams/definitions").description("Exposes the Streams resource"),
				linkWithRel("streams/definitions/definition").description("Handle a specific Stream definition"),
				linkWithRel("streams/validation").description("Provides the validation for a stream definition"),
				linkWithRel("streams/deployments").description("Provides Stream deployment operations"),
				linkWithRel("streams/deployments/{name}").description("Request deployment info for a stream definition"),
				linkWithRel("streams/deployments/{name}{?reuse-deployment-properties}").description("Request deployment info for a stream definition"),
				linkWithRel("streams/deployments/deployment").description("Request (un-)deployment of an existing stream definition"),
				linkWithRel("streams/deployments/manifest/{name}/{version}").description("Return a manifest info of a release version"),
				linkWithRel("streams/deployments/history/{name}").description("Get stream's deployment history as list or Releases for this release"),
				linkWithRel("streams/deployments/rollback/{name}/{version}").description("Rollback the stream to the previous or a specific version of the stream"),
				linkWithRel("streams/deployments/update/{name}").description("Update the stream."),
				linkWithRel("streams/deployments/platform/list").description("List of supported deployment platforms"),
				linkWithRel("streams/deployments/scale/{streamName}/{appName}/instances/{count}").description("Scale up or down number of application instances for a selected stream"),
				linkWithRel("streams/logs").description("Retrieve application logs of the stream"),
				linkWithRel("streams/logs/{streamName}").description("Retrieve application logs of the stream"),
				linkWithRel("streams/logs/{streamName}/{appName}").description("Retrieve a specific application log of the stream"),

				linkWithRel("tools/parseTaskTextToGraph").description("Parse a task definition into a graph structure"),
				linkWithRel("tools/convertTaskGraphToText").description("Convert a graph format into " + "DSL text format")),

				responseFields(
						fieldWithPath("_links").description("Links to other resources"),
						fieldWithPath("['" + Version.REVISION_KEY + "']").description("Incremented each time a change is implemented in this REST API"),
						fieldWithPath("_links.audit-records.href").description("Link to the audit records"),
						fieldWithPath("_links.dashboard.href").description("Link to the dashboard"),

						fieldWithPath("_links.schema/versions.href").description("Link to the schema/versions"),
						fieldWithPath("_links.schema/targets.href").description("Link to the schema/targets"),

						fieldWithPath("_links.streams/definitions.href").description("Link to the streams/definitions"),
						fieldWithPath("_links.streams/definitions/definition.href").description("Link to the streams/definitions/definition"),
						fieldWithPath("_links.streams/definitions/definition.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/definitions/definition is templated"),

						fieldWithPath("_links.runtime/apps.href").description("Link to the runtime/apps"),
						fieldWithPath("_links.runtime/apps/{appId}.href").description("Link to the runtime/apps/{appId}"),
						fieldWithPath("_links.runtime/apps/{appId}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link runtime/apps is templated"),
						fieldWithPath("_links.runtime/apps/{appId}/instances.href").description("Link to the runtime/apps/{appId}/instances"),
						fieldWithPath("_links.runtime/apps/{appId}/instances.templated").type(JsonFieldType.BOOLEAN).optional().description("Link runtime/apps/{appId}/instances is templated"),
						fieldWithPath("_links.runtime/apps/{appId}/instances/{instanceId}.href").description("Link to the runtime/apps/{appId}/instances/{instanceId}"),
						fieldWithPath("_links.runtime/apps/{appId}/instances/{instanceId}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link runtime/apps/{appId}/instances/{instanceId} is templated"),
						fieldWithPath("_links.runtime/apps/{appId}/instances/{instanceId}/post.href").description("Link to the runtime/apps/{appId}/instances/{instanceId}/post"),
						fieldWithPath("_links.runtime/apps/{appId}/instances/{instanceId}/post.templated").type(JsonFieldType.BOOLEAN).optional().description("Link runtime/apps/{appId}/instances/{instanceId}/post is templated"),

						fieldWithPath("_links.runtime/apps/{appId}/instances/{instanceId}/actuator[].href").description("Link to the runtime/apps/{appId}/instances/{instanceId}/actuator"),
						fieldWithPath("_links.runtime/apps/{appId}/instances/{instanceId}/actuator[].templated").type(JsonFieldType.BOOLEAN).optional().description("Link runtime/apps/{appId}/instances/{instanceId}/actuator is templated"),

						fieldWithPath("_links.runtime/streams.href").description("Link to the runtime/streams"),
						fieldWithPath("_links.runtime/streams.templated").type(JsonFieldType.BOOLEAN).optional().description("Link runtime/streams is templated"),
						fieldWithPath("_links.runtime/streams/{streamNames}.href").description("Link to the runtime/streams/{streamNames}"),
						fieldWithPath("_links.runtime/streams/{streamNames}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link runtime/streams/{streamNames} is templated"),

						fieldWithPath("_links.streams/logs.href").description("Link to the streams/logs"),
						fieldWithPath("_links.streams/logs/{streamName}.href").description("Link to the streams/logs/{streamName}"),
						fieldWithPath("_links.streams/logs/{streamName}/{appName}.href").description("Link to the streams/logs/{streamName}/{appName}"),
						fieldWithPath("_links.streams/logs/{streamName}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/logs/{streamName} is templated"),
						fieldWithPath("_links.streams/logs/{streamName}/{appName}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/logs/{streamName}/{appName} is templated"),

						fieldWithPath("_links.streams/deployments").description("Link to streams/deployments"),
						fieldWithPath("_links.streams/deployments.href").description("Link to streams/deployments"),
						fieldWithPath("_links.streams/deployments/{name}").description("Link streams/deployments/{name} is templated"),
						fieldWithPath("_links.streams/deployments/{name}.href").description("Link streams/deployments/{name} is templated"),
						fieldWithPath("_links.streams/deployments/{name}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/{name} is templated"),
						fieldWithPath("_links.streams/deployments/{name}{?reuse-deployment-properties}.href").description("Link streams/deployments/{name} is templated"),
						fieldWithPath("_links.streams/deployments/{name}{?reuse-deployment-properties}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/{name} is templated"),
						fieldWithPath("_links.streams/deployments/deployment.href").description("Link to the streams/deployments/deployment"),
						fieldWithPath("_links.streams/deployments/deployment.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/deployment is templated"),
						fieldWithPath("_links.streams/deployments/manifest/{name}/{version}.href").description("Link to the streams/deployments/manifest/{name}/{version}"),
						fieldWithPath("_links.streams/deployments/manifest/{name}/{version}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/manifest/{name}/{version} is templated"),
						fieldWithPath("_links.streams/deployments/history/{name}.href").description("Link to the streams/deployments/history/{name}"),
						fieldWithPath("_links.streams/deployments/history/{name}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/history is templated"),
						fieldWithPath("_links.streams/deployments/rollback/{name}/{version}.href").description("Link to the streams/deployments/rollback/{name}/{version}"),
						fieldWithPath("_links.streams/deployments/rollback/{name}/{version}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/rollback/{name}/{version} is templated"),
						fieldWithPath("_links.streams/deployments/update/{name}.href").description("Link to the streams/deployments/update/{name}"),
						fieldWithPath("_links.streams/deployments/update/{name}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/update/{name} is templated"),
						fieldWithPath("_links.streams/deployments/platform/list.href").description("Link to the streams/deployments/platform/list"),
						fieldWithPath("_links.streams/deployments/scale/{streamName}/{appName}/instances/{count}.href").description("Link to the streams/deployments/scale/{streamName}/{appName}/instances/{count}"),
						fieldWithPath("_links.streams/deployments/scale/{streamName}/{appName}/instances/{count}.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/deployments/scale/{streamName}/{appName}/instances/{count} is templated"),

						fieldWithPath("_links.streams/validation.href").description("Link to the streams/validation"),
						fieldWithPath("_links.streams/validation.templated").type(JsonFieldType.BOOLEAN).optional().description("Link streams/validation is templated"),

						fieldWithPath("_links.tasks/platforms.href").description("Link to the tasks/platforms"),

						fieldWithPath("_links.tasks/definitions.href").description("Link to the tasks/definitions"),
						fieldWithPath("_links.tasks/definitions/definition.href").description("Link to the tasks/definitions/definition"),
						fieldWithPath("_links.tasks/definitions/definition.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/definitions/definition is templated"),

						fieldWithPath("_links.tasks/executions.href").description("Link to the tasks/executions"),
						fieldWithPath("_links.tasks/executions/launch.href").description("Link to tasks/executions/launch"),
						fieldWithPath("_links.tasks/executions/launch.templated").type(JsonFieldType.BOOLEAN).optional().description("Indicates that Link tasks/executions/launch is templated"),
						fieldWithPath("_links.tasks/executions/name.href").description("Link to the tasks/executions/name"),
						fieldWithPath("_links.tasks/executions/name.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/executions/name is templated"),
						fieldWithPath("_links.tasks/executions/current.href").description("Link to the tasks/executions/current"),
						fieldWithPath("_links.tasks/executions/execution.href").description("Link to the tasks/executions/execution"),
						fieldWithPath("_links.tasks/executions/execution.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/executions/execution is templated"),
						fieldWithPath("_links.tasks/executions/external.href").description("Link to the tasks/executions/external"),
						fieldWithPath("_links.tasks/executions/external.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/executions/external is templated"),

						fieldWithPath("_links.tasks/info/executions.href").description("Link to the tasks/info/executions"),
						fieldWithPath("_links.tasks/info/executions.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/info is templated"),

						fieldWithPath("_links.tasks/logs.href").description("Link to the tasks/logs"),
						fieldWithPath("_links.tasks/logs.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/logs is templated"),

						fieldWithPath("_links.tasks/thinexecutions.href").description("Link to the tasks/thinexecutions"),

						fieldWithPath("_links.tasks/thinexecutions/name.href").description("Link to the tasks/thinexecutions/name"),
						fieldWithPath("_links.tasks/thinexecutions/name.templated").type(JsonFieldType.BOOLEAN).optional().description("Link to the tasks/thinexecutions/name is templated"),

						fieldWithPath("_links.tasks/schedules.href").description("Link to the tasks/executions/schedules"),
						fieldWithPath("_links.tasks/schedules/instances.href").description("Link to the tasks/schedules/instances"),
						fieldWithPath("_links.tasks/schedules/instances.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/schedules/instances is templated"),

						fieldWithPath("_links.tasks/validation.href").description("Link to the tasks/validation"),
						fieldWithPath("_links.tasks/validation.templated").type(JsonFieldType.BOOLEAN).optional().description("Link tasks/validation is templated"),

						fieldWithPath("_links.jobs/executions.href").description("Link to the jobs/executions"),

						fieldWithPath("_links.jobs/thinexecutions.href").description("Link to the jobs/thinexecutions"),

						fieldWithPath("_links.jobs/executions/name.href").description("Link to the jobs/executions/name"),
						fieldWithPath("_links.jobs/executions/name.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/name is templated"),
						fieldWithPath("_links.jobs/executions/status.href").description("Link to the jobs/executions/status"),
						fieldWithPath("_links.jobs/executions/status.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/status is templated"),

						fieldWithPath("_links.jobs/thinexecutions/name.href").description("Link to the jobs/thinexecutions/name"),
						fieldWithPath("_links.jobs/thinexecutions/name.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/name is templated"),
						fieldWithPath("_links.jobs/thinexecutions/jobInstanceId.href").description("Link to the jobs/thinexecutions/jobInstanceId"),
						fieldWithPath("_links.jobs/thinexecutions/jobInstanceId.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/jobInstanceId is templated"),
						fieldWithPath("_links.jobs/thinexecutions/taskExecutionId.href").description("Link to the jobs/thinexecutions/taskExecutionId"),
						fieldWithPath("_links.jobs/thinexecutions/taskExecutionId.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/taskExecutionId is templated"),

						fieldWithPath("_links.jobs/executions/execution.href").description("Link to the jobs/executions/execution"),
						fieldWithPath("_links.jobs/executions/execution.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/execution is templated"),
						fieldWithPath("_links.jobs/executions/execution/steps.href").description("Link to the jobs/executions/execution/steps"),
						fieldWithPath("_links.jobs/executions/execution/steps.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/execution/steps is templated"),
						fieldWithPath("_links.jobs/executions/execution/steps/step.href").description("Link to the jobs/executions/execution/steps/step"),
						fieldWithPath("_links.jobs/executions/execution/steps/step.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/execution/steps/step is templated"),
						fieldWithPath("_links.jobs/executions/execution/steps/step/progress.href").description("Link to the jobs/executions/execution/steps/step/progress"),
						fieldWithPath("_links.jobs/executions/execution/steps/step/progress.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/executions/execution/steps/step/progress is templated"),

						fieldWithPath("_links.jobs/instances/name.href").description("Link to the jobs/instances/name"),
						fieldWithPath("_links.jobs/instances/name.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/instances/name is templated"),
						fieldWithPath("_links.jobs/instances/instance.href").description("Link to the jobs/instances/instance"),
						fieldWithPath("_links.jobs/instances/instance.templated").type(JsonFieldType.BOOLEAN).optional().description("Link jobs/instances/instance is templated"),

						fieldWithPath("_links.tools/parseTaskTextToGraph.href").description("Link to the tools/parseTaskTextToGraph"),
						fieldWithPath("_links.tools/convertTaskGraphToText.href").description("Link to the tools/convertTaskGraphToText"),

						fieldWithPath("_links.apps.href").description("Link to the apps"),

						fieldWithPath("_links.about.href").description("Link to the about"),

						fieldWithPath("_links.completions/stream.href").description("Link to the completions/stream"),
						fieldWithPath("_links.completions/stream.templated").type(JsonFieldType.BOOLEAN).optional().description("Link completions/stream is templated"),
						fieldWithPath("_links.completions/task.href").description("Link to the completions/task"),
						fieldWithPath("_links.completions/task.templated").type(JsonFieldType.BOOLEAN).optional().description("Link completions/task is templated")
				)));
	}
}
