/*
 * Copyright 2016 the original author or authors.
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

import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionThinResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionProgressInfoResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Implementation for {@link JobOperations}.
 *
 * @author Glenn Renfro
 */
public class JobTemplate implements JobOperations {

	private static final String EXECUTIONS_THIN_RELATION = "jobs/thinexecutions";
	private static final String EXECUTIONS_RELATION = "jobs/executions";
	private static final String EXECUTION_RELATION = "jobs/executions/execution";
	private static final String EXECUTION_RELATION_BY_NAME = "jobs/executions/name";
	private static final String EXECUTION_THIN_RELATION_BY_NAME = "jobs/thinexecutions/name";

	private static final String INSTANCE_RELATION = "jobs/instances/instance";

	private static final String INSTANCE_RELATION_BY_NAME = "jobs/instances/name";

	private static final String STEP_EXECUTION_RELATION_BY_ID = "jobs/executions/execution/steps";

	private static final String STEP_EXECUTION_PROGRESS_RELATION_BY_ID = "jobs/executions/execution/steps/step/progress";

	private final RestTemplate restTemplate;

	private final Link executionsLink;
	private final Link thinExecutionsLink;

	private final Link thinExecutionByNameLink;

	private final Link executionLink;

	private final Link executionByNameLink;

	private final Link instanceLink;

	private final Link instanceByNameLink;

	private final Link stepExecutionsLink;

	private final Link stepExecutionProgressLink;

	JobTemplate(RestTemplate restTemplate, RepresentationModel<?> resources) {
		Assert.notNull(resources, "URI CollectionModel must not be be null");
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		Assert.isTrue(resources.getLink(EXECUTIONS_RELATION).isPresent(), "Executions relation is required");
		Assert.isTrue(resources.getLink(EXECUTIONS_THIN_RELATION).isPresent(), "Executions thin relation is required");
		Assert.isTrue(resources.getLink(EXECUTION_THIN_RELATION_BY_NAME).isPresent(), "Executions thin relation is required");
		Assert.isTrue(resources.getLink(EXECUTION_RELATION).isPresent(), "Execution relation is required");
		Assert.isTrue(resources.getLink(EXECUTION_RELATION_BY_NAME).isPresent(), "Execution by name relation is required");
		Assert.isTrue(resources.getLink(INSTANCE_RELATION).isPresent(), "Instance relation is required");
		Assert.isTrue(resources.getLink(INSTANCE_RELATION_BY_NAME).isPresent(), "Instance by name relation is required");
		Assert.isTrue(resources.getLink(STEP_EXECUTION_RELATION_BY_ID).isPresent(), "Step Execution by id relation is required");
		Assert.isTrue(resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID).isPresent(),
				"Step Execution Progress by id " + "relation is required");
		Assert.isTrue(resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID).isPresent(),
				"Step Execution View by id relation" + " is required");

		this.restTemplate = restTemplate;
		this.executionsLink = resources.getLink(EXECUTIONS_RELATION).get();
		this.thinExecutionsLink = resources.getLink(EXECUTIONS_THIN_RELATION).get();
		this.executionLink = resources.getLink(EXECUTION_RELATION).get();
		this.executionByNameLink = resources.getLink(EXECUTION_RELATION_BY_NAME).get();
		this.thinExecutionByNameLink = resources.getLink(EXECUTION_THIN_RELATION_BY_NAME).get();
		this.instanceLink = resources.getLink(INSTANCE_RELATION).get();
		this.instanceByNameLink = resources.getLink(INSTANCE_RELATION_BY_NAME).get();
		this.stepExecutionsLink = resources.getLink(STEP_EXECUTION_RELATION_BY_ID).get();
		this.stepExecutionProgressLink = resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID).get();
	}

	@Override
	public PagedModel<JobExecutionResource> executionList() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(executionsLink.getHref()).queryParam("size", "2000");
		return restTemplate.getForObject(builder.toUriString(), JobExecutionResource.Page.class);
	}

	@Override
	public void executionRestart(long id) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(executionLink.expand(id).getHref()).queryParam("restart", "true");

		restTemplate.put(builder.toUriString(), null);
	}

	@Override
	public void executionRestart(long id, Boolean useJsonJobParameters) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(executionLink.expand(id).getHref()).queryParam("restart", "true")
			.queryParam("useJsonJobParameters", useJsonJobParameters);

		restTemplate.put(builder.toUriString(), null);
	}

	@Override
	public PagedModel<JobExecutionThinResource> executionThinList() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(thinExecutionsLink.getHref()).queryParam("size", "2000");
		return restTemplate.getForObject(builder.toUriString(), JobExecutionThinResource.Page.class);
	}

	@Override
	public PagedModel<JobInstanceResource> instanceList(String jobName) {
		return restTemplate.getForObject(instanceByNameLink.expand(jobName).getHref(), JobInstanceResource.Page.class);
	}

	@Override
	public PagedModel<JobExecutionThinResource> executionThinListByJobName(String jobName) {
		return restTemplate.getForObject(thinExecutionByNameLink.expand(jobName).getHref(), JobExecutionThinResource.Page.class);
	}

	@Override
	public PagedModel<JobExecutionResource> executionListByJobName(String jobName) {
		return restTemplate.getForObject(executionByNameLink.expand(jobName).getHref(), JobExecutionResource.Page.class);
	}

	@Override
	public JobExecutionResource jobExecution(long id) {
		String url = executionLink.expand(id).getHref();
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);

		return restTemplate.getForObject(builder.toUriString(), JobExecutionResource.class);
	}

	@Override
	public JobInstanceResource jobInstance(long id) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(instanceLink.expand(id).getHref());
		return restTemplate.getForObject(builder.toUriString(), JobInstanceResource.class);
	}

	@Override
	public PagedModel<StepExecutionResource> stepExecutionList(long jobExecutionId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(stepExecutionsLink.expand(jobExecutionId).getHref());
		return restTemplate.getForObject(builder.toUriString(), StepExecutionResource.Page.class);
	}

	@Override
	public StepExecutionProgressInfoResource stepExecutionProgress(long jobExecutionId, long stepExecutionId) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(stepExecutionProgressLink.expand(jobExecutionId, stepExecutionId).getHref());
		return restTemplate.getForObject(builder.toUriString(), StepExecutionProgressInfoResource.class);
	}

}
