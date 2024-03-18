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

package org.springframework.cloud.dataflow.server.job.support;

import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.cloud.dataflow.rest.job.support.StepType;
import org.springframework.cloud.dataflow.rest.job.support.TaskletType;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.cloud.dataflow.server.batch.NoSuchStepExecutionException;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionController;
import org.springframework.cloud.dataflow.server.controller.JobStepExecutionProgressController;
import org.springframework.util.Assert;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Knows how to build a StepExecutionResource out of our domain model
 * {@link StepExecution}.
 *
 * @author Glenn Renfro
 * @since 1.0
 */
public class StepExecutionResourceBuilder {

	static public StepExecutionResource toModel(StepExecution entity) {
		StepExecutionResource resource = new StepExecutionResource(entity.getJobExecution().getId(), entity, generateStepType(entity));
		try {
			resource.add(
					linkTo(
							methodOn(JobStepExecutionController.class)
									.getStepExecution(resource.getStepExecution().getJobExecutionId(), resource.getStepExecution().getId())
					).withSelfRel()
			);
			resource.add(
					linkTo(
							methodOn(JobStepExecutionProgressController.class)
									.progress(resource.getStepExecution().getJobExecutionId(), resource.getStepExecution().getId())
					).withRel("progress")
			);
		} catch (NoSuchStepExecutionException | NoSuchJobExecutionException e) {
			throw new RuntimeException(e);
		}
		return resource;
	}

	private static String generateStepType(StepExecution stepExecution) {
		Assert.notNull(stepExecution, "stepExecution must not be null");
		String stepType = StepType.UNKNOWN.getDisplayName();
		if (stepExecution.getExecutionContext().containsKey(TaskletStep.TASKLET_TYPE_KEY)) {
			String taskletClassName = stepExecution.getExecutionContext().getString(TaskletStep.TASKLET_TYPE_KEY);
			TaskletType type = TaskletType.fromClassName(taskletClassName);

			if (type == TaskletType.UNKNOWN) {
				stepType = taskletClassName;
			}
			else {
				stepType = type.getDisplayName();
			}
		}
		else if (stepExecution.getExecutionContext().containsKey(Step.STEP_TYPE_KEY)) {
			String stepClassName = stepExecution.getExecutionContext().getString(Step.STEP_TYPE_KEY);
			StepType type = StepType.fromClassName(stepClassName);

			if (type == StepType.UNKNOWN) {
				stepType = stepClassName;
			}
			else {
				stepType = type.getDisplayName();
			}
		}
		return stepType;
	}

}
