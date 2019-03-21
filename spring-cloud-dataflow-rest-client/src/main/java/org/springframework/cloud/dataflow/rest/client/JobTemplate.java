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
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

/**
 * Implementation for {@link JobOperations}.
 *
 * @author Glenn Renfro
 */
public class JobTemplate implements JobOperations {

	private static final String EXECUTIONS_RELATION = "jobs/thinexecutions";

	private static final String EXECUTION_RELATION = "jobs/executions/execution";

	private static final String EXECUTION_RELATION_BY_NAME = "jobs/thinexecutions/name";

	private static final String INSTANCE_RELATION = "jobs/instances/instance";

	private static final String INSTANCE_RELATION_BY_NAME = "jobs/instances/name";

	private static final String STEP_EXECUTION_RELATION_BY_ID = "jobs/executions/execution/steps";

	private static final String STEP_EXECUTION_PROGRESS_RELATION_BY_ID = "jobs/executions/execution/steps/step/progress";

	private final RestTemplate restTemplate;

	private final Link executionsLink;

	private final Link executionLink;

	private final Link executionByNameLink;

	private final Link instanceLink;

	private final Link instanceByNameLink;

	private final Link stepExecutionsLink;

	private final Link stepExecutionProgressLink;

	private final Link stepExecutionLink;

	JobTemplate(RestTemplate restTemplate, ResourceSupport resources) {
		Assert.notNull(resources, "URI Resources must not be be null");
		Assert.notNull(restTemplate, "RestTemplate must not be null");
		Assert.notNull(resources.getLink(EXECUTIONS_RELATION), "Executions relation is required");
		Assert.notNull(resources.getLink(EXECUTION_RELATION), "Execution relation is required");
		Assert.notNull(resources.getLink(EXECUTION_RELATION_BY_NAME), "Execution by name relation is required");
		Assert.notNull(resources.getLink(INSTANCE_RELATION), "Instance relation is required");
		Assert.notNull(resources.getLink(INSTANCE_RELATION_BY_NAME), "Instance by name relation is required");
		Assert.notNull(resources.getLink(STEP_EXECUTION_RELATION_BY_ID), "Step Execution by id relation is required");
		Assert.notNull(resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID),
				"Step Execution Progress by id " + "relation is required");
		Assert.notNull(resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID),
				"Step Execution View by id relation" + " is required");

		this.restTemplate = restTemplate;
		this.executionsLink = resources.getLink(EXECUTIONS_RELATION);
		this.executionLink = resources.getLink(EXECUTION_RELATION);
		this.executionByNameLink = resources.getLink(EXECUTION_RELATION_BY_NAME);
		this.instanceLink = resources.getLink(INSTANCE_RELATION);
		this.instanceByNameLink = resources.getLink(INSTANCE_RELATION_BY_NAME);
		this.stepExecutionsLink = resources.getLink(STEP_EXECUTION_RELATION_BY_ID);
		this.stepExecutionProgressLink = resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID);
		this.stepExecutionLink = resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID);

	}

	@Override
	public PagedResources<JobExecutionResource> executionList() {
		String uriTemplate = executionsLink.getHref().toString();
		uriTemplate = uriTemplate + "?size=2000";

		return restTemplate.getForObject(uriTemplate, JobExecutionResource.Page.class);
	}

	@Override
	public PagedResources<JobExecutionThinResource> executionThinList() {
		String uriTemplate = executionsLink.getHref().toString();
		uriTemplate = uriTemplate + "?size=2000";

		return restTemplate.getForObject(uriTemplate, JobExecutionThinResource.Page.class);
	}

	@Override
	public PagedResources<JobInstanceResource> instanceList(String jobName) {
		return restTemplate.getForObject(instanceByNameLink.expand(jobName).getHref(), JobInstanceResource.Page.class);
	}

	@Override
	public PagedResources<JobExecutionThinResource> executionThinListByJobName(String jobName) {
		return restTemplate.getForObject(executionByNameLink.expand(jobName).getHref(),
				JobExecutionThinResource.Page.class);
	}

	@Override
	public PagedResources<JobExecutionResource> executionListByJobName(String jobName) {
		return restTemplate.getForObject(executionByNameLink.expand(jobName).getHref(),
				JobExecutionResource.Page.class);
	}

	@Override
	public JobExecutionResource jobExecution(long id) {
		return restTemplate.getForObject(executionLink.expand(id).getHref(), JobExecutionResource.class);
	}

	@Override
	public JobInstanceResource jobInstance(long id) {
		return restTemplate.getForObject(instanceLink.expand(id).getHref(), JobInstanceResource.class);
	}

	@Override
	public PagedResources<StepExecutionResource> stepExecutionList(long jobExecutionId) {
		return restTemplate.getForObject(stepExecutionsLink.expand(jobExecutionId).getHref(),
				StepExecutionResource.Page.class);
	}

	@Override
	public StepExecutionProgressInfoResource stepExecutionProgress(long jobExecutionId, long stepExecutionId) {
		return restTemplate.getForObject(stepExecutionProgressLink.expand(jobExecutionId, stepExecutionId).getHref(),
				StepExecutionProgressInfoResource.class);
	}

}
