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

package org.springframework.cloud.dataflow.server.controller;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionProgressInfoResource;
import org.springframework.cloud.dataflow.schema.SchemaVersionTarget;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.batch.NoSuchStepExecutionException;
import org.springframework.cloud.dataflow.server.job.support.StepExecutionProgressInfo;
import org.springframework.cloud.dataflow.server.service.JobServiceContainer;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * @author Glenn Renfro
 * @author Corneil du Plessis
 */
@RestController
@RequestMapping("/jobs/executions/{jobExecutionId}/steps")
@ExposesResourceFor(StepExecutionProgressInfoResource.class)
public class JobStepExecutionProgressController {

	private final TaskJobService taskJobService;

	private final JobServiceContainer jobServiceContainer;

	/**
	 * Creates a {@code JobStepProgressInfoExecutionsController} that retrieves Job Step
	 * Progress Execution information from a the {@link JobServiceContainer}
	 *
	 * @param jobServiceContainer A container of JobServices that this controller will use for retrieving job step
	 *                            progress execution information.
	 * @param taskJobService      Queries both schemas.
	 */
	@Autowired
	public JobStepExecutionProgressController(JobServiceContainer jobServiceContainer, TaskJobService taskJobService) {
		this.taskJobService = taskJobService;
		this.jobServiceContainer = jobServiceContainer;
	}

	/**
	 * Get the step execution progress for the given jobExecutions step.
	 *
	 * @param jobExecutionId  Id of the {@link JobExecution}, must not be null
	 * @param stepExecutionId Id of the {@link StepExecution}, must not be null
	 * @return {@link StepExecutionProgressInfoResource} that has the progress info on the
	 * given {@link StepExecution}.
	 * @throws NoSuchJobExecutionException  Thrown if the respective {@link JobExecution}
	 *                                      does not exist
	 * @throws NoSuchStepExecutionException Thrown if the respective {@link StepExecution}
	 *                                      does not exist
	 */
	@RequestMapping(value = "/{stepExecutionId}/progress", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public StepExecutionProgressInfoResource progress(
			@PathVariable long jobExecutionId,
			@PathVariable long stepExecutionId,
			@RequestParam(name = "schemaTarget", required = false) String schemaTarget
	) throws NoSuchStepExecutionException, NoSuchJobExecutionException {
		try {
			if (!StringUtils.hasText(schemaTarget)) {
				schemaTarget = SchemaVersionTarget.defaultTarget().getName();
			}
			JobService jobService = jobServiceContainer.get(schemaTarget);
			StepExecution stepExecution = jobService.getStepExecution(jobExecutionId, stepExecutionId);
			String stepName = stepExecution.getStepName();
			if (stepName.contains(":partition")) {
				// assume we want to compare all partitions
				stepName = stepName.replaceAll("(:partition).*", "$1*");
			}
			String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
			StepExecutionHistory stepExecutionHistory = computeHistory(jobName, stepName, schemaTarget);
			final Assembler stepAssembler = new Assembler(schemaTarget);
			return stepAssembler.toModel(new StepExecutionProgressInfo(stepExecution, stepExecutionHistory));
		} catch (NoSuchStepExecutionException e) {
			throw new NoSuchStepExecutionException(String.valueOf(stepExecutionId));
		} catch (NoSuchJobExecutionException e) {
			throw new NoSuchJobExecutionException(String.valueOf(jobExecutionId));
		}
	}

	/**
	 * Compute step execution history for the given jobs step.
	 *
	 * @param jobName  the name of the job
	 * @param stepName the name of the step
	 * @return the step execution history for the given step
	 */
	private StepExecutionHistory computeHistory(String jobName, String stepName, String schemaTarget) {
		JobService jobService = jobServiceContainer.get(schemaTarget);
		int total = jobService.countStepExecutionsForStep(jobName, stepName);
		StepExecutionHistory stepExecutionHistory = new StepExecutionHistory(stepName);
		for (int i = 0; i < total; i += 1000) {
			for (StepExecution stepExecution : jobService.listStepExecutionsForStep(jobName, stepName, i, 1000)) {
				stepExecutionHistory.append(stepExecution);
			}
		}
		return stepExecutionHistory;
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that converts
	 * {@link StepExecutionProgressInfo}s to a {@link StepExecutionProgressInfoResource}.
	 */
	private static class Assembler
			extends RepresentationModelAssemblerSupport<StepExecutionProgressInfo, StepExecutionProgressInfoResource> {
		private final String schemaTarget;

		public Assembler(String schemaTarget) {
			super(JobStepExecutionProgressController.class, StepExecutionProgressInfoResource.class);
			this.schemaTarget = schemaTarget;
		}

		@Override
		public StepExecutionProgressInfoResource toModel(StepExecutionProgressInfo entity) {
			return createModelWithId(entity.getStepExecutionId(), entity,
					entity.getStepExecution().getJobExecutionId());
		}

		@Override
		protected StepExecutionProgressInfoResource instantiateModel(StepExecutionProgressInfo entity) {
			StepExecutionProgressInfoResource resource = new StepExecutionProgressInfoResource(entity.getStepExecution(), entity.getStepExecutionHistory(),
					entity.getEstimatedPercentComplete(), entity.isFinished(), entity.getDuration());
			addLink(resource);
			return resource;
		}

		private void addLink(StepExecutionProgressInfoResource resource) {
			try {
				resource.add(
						linkTo(
								methodOn(JobStepExecutionProgressController.class)
										.progress(resource.getStepExecution().getJobExecutionId(), resource.getStepExecution().getId(), schemaTarget)
						).withRel("progress")
				);
			} catch (NoSuchStepExecutionException | NoSuchJobExecutionException e) {
				throw new RuntimeException(e);
			}
		}
	}
}
