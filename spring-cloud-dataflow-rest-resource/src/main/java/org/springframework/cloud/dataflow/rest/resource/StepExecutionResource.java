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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.batch.core.StepExecution;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.util.Assert;

/**
 * @author Glenn Renfro
 */
public class StepExecutionResource extends RepresentationModel<StepExecutionResource> {

	private final Long jobExecutionId;

	private final StepExecution stepExecution;

	private final String stepType;

	/**
	 * Create a new StepExecutionResource
	 *
	 * @param jobExecutionId the job execution id, must not be null
	 * @param stepExecution the step execution, must not be null
	 * @param stepType the step type
	 */
	public StepExecutionResource(Long jobExecutionId, StepExecution stepExecution, String stepType) {

		Assert.notNull(jobExecutionId, "jobExecutionId must not be null.");
		Assert.notNull(stepExecution, "stepExecution must not be null.");

		this.stepExecution = stepExecution;
		this.jobExecutionId = jobExecutionId;
		this.stepType = stepType;
	}

	/**
	 * Default constructor to be used by Jackson.
	 */
	private StepExecutionResource() {
		this.stepExecution = null;
		this.jobExecutionId = null;
		this.stepType = null;
		}

	/**
	 * @return The jobExecutionId, which will never be null
	 */
	public Long getJobExecutionId() {
		return this.jobExecutionId;
	}

	/**
	 * @return The stepExecution, which will never be null
	 */
	public StepExecution getStepExecution() {
		return stepExecution;
	}

	public String getStepType() {
		return this.stepType;
	}

	public static class Page extends PagedModel<StepExecutionResource> {
	}

}
