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

import java.time.temporal.ValueRange;

import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionThinResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionProgressInfoResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
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

	JobTemplate(RestTemplate restTemplate, RepresentationModel<?> resources) {
		Assert.notNull(resources, "URI CollectionModel must not be be null");
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
		this.executionsLink = resources.getLink(EXECUTIONS_RELATION).get();
		this.executionLink = resources.getLink(EXECUTION_RELATION).get();
		this.executionByNameLink = resources.getLink(EXECUTION_RELATION_BY_NAME).get();
		this.instanceLink = resources.getLink(INSTANCE_RELATION).get();
		this.instanceByNameLink = resources.getLink(INSTANCE_RELATION_BY_NAME).get();
		this.stepExecutionsLink = resources.getLink(STEP_EXECUTION_RELATION_BY_ID).get();
		this.stepExecutionProgressLink = resources.getLink(STEP_EXECUTION_PROGRESS_RELATION_BY_ID).get();
	}

	@Override
	public PagedModel<JobExecutionResource> executionList() {
		String uriTemplate = executionsLink.getHref();
		uriTemplate = uriTemplate + "?size=2000";

		return restTemplate.getForObject(uriTemplate, JobExecutionResource.Page.class);
	}

	@Override
	public void executionRestart(long id, String schemaTarget) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		if(schemaTarget != null) {
			values.add("schemaTarget", schemaTarget);
		}
		values.add("restart", "true");
		String uriTemplate = executionLink.expand(id).getHref();

		restTemplate.put(uriTemplate, null, values);
	}

	@Override
	public PagedModel<JobExecutionThinResource> executionThinList() {
		String uriTemplate = executionsLink.getHref();
		uriTemplate = uriTemplate + "?size=2000";

		return restTemplate.getForObject(uriTemplate, JobExecutionThinResource.Page.class);
	}

	@Override
	public PagedModel<JobInstanceResource> instanceList(String jobName) {
		return restTemplate.getForObject(instanceByNameLink.expand(jobName).getHref(), JobInstanceResource.Page.class);
	}

	@Override
	public PagedModel<JobExecutionThinResource> executionThinListByJobName(String jobName) {
		return restTemplate.getForObject(executionByNameLink.expand(jobName).getHref(),
				JobExecutionThinResource.Page.class);
	}

	@Override
	public PagedModel<JobExecutionResource> executionListByJobName(String jobName) {
		return restTemplate.getForObject(executionByNameLink.expand(jobName).getHref(),
				JobExecutionResource.Page.class);
	}

	@Override
	public JobExecutionResource jobExecution(long id, String schemaTarget) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		if(StringUtils.hasText(schemaTarget)) {
			values.add("schemaTarget", schemaTarget);
		}
		return restTemplate.getForObject(executionLink.expand(id).getHref(), JobExecutionResource.class, values);
	}

	@Override
	public JobInstanceResource jobInstance(long id, String schemaTarget) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		if(StringUtils.hasText(schemaTarget)) {
			values.add("schemaTarget", schemaTarget);
		}
		return restTemplate.getForObject(instanceLink.expand(id).getHref(), JobInstanceResource.class, values);
	}

	@Override
	public PagedModel<StepExecutionResource> stepExecutionList(long jobExecutionId, String schemaTarget) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		if(StringUtils.hasText(schemaTarget)) {
			values.add("schemaTarget", schemaTarget);
		}
		return restTemplate.getForObject(stepExecutionsLink.expand(jobExecutionId).getHref(),
				StepExecutionResource.Page.class, values);
	}

	@Override
	public StepExecutionProgressInfoResource stepExecutionProgress(long jobExecutionId, long stepExecutionId, String schemaTarget) {
		MultiValueMap<String, Object> values = new LinkedMultiValueMap<>();
		if(StringUtils.hasText(schemaTarget)) {
			values.add("schemaTarget", schemaTarget);
		}
		return restTemplate.getForObject(stepExecutionProgressLink.expand(jobExecutionId, stepExecutionId).getHref(),
				StepExecutionProgressInfoResource.class, values);
	}

}
