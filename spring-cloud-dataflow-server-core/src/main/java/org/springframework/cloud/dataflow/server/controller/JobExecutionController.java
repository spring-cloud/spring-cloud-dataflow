/*
 * Copyright 2016=2023 the original author or authors.
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

import java.util.TimeZone;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.TimeUtils;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Controller for operations on {@link org.springframework.batch.core.JobExecution}. This
 * includes obtaining Job execution information from the job explorer.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 * @author Corneil du Plessis
 */
@RestController
@RequestMapping("/jobs/executions")
@ExposesResourceFor(JobExecutionResource.class)
public class JobExecutionController {

	private final Assembler jobAssembler = new Assembler();

	private final TaskJobService taskJobService;

	/**
	 * Creates a {@code JobExecutionController} that retrieves Job Execution information from
	 * a the {@link JobService}
	 *
	 * @param taskJobService the service this controller will use for retrieving job execution
	 *                       information. Must not be null.
	 */
	public JobExecutionController(TaskJobService taskJobService) {
		Assert.notNull(taskJobService, "taskJobService must not be null");
		this.taskJobService = taskJobService;
	}

	/**
	 * Retrieve all task job executions with the task name specified
	 *
	 * @param jobName   name of the job. SQL server specific wildcards are enabled (eg.: myJob%,
	 *                  m_Job, ...)
	 * @param status    Optional status criteria.
	 * @param pageable  page-able collection of {@code TaskJobExecution}s.
	 * @param assembler for the {@link TaskJobExecution}s
	 * @return list task/job executions with the specified jobName.
	 * @throws NoSuchJobException          if the job with the given name does not exist.
	 * @throws NoSuchJobExecutionException if the job execution doesn't exist.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<JobExecutionResource> retrieveJobsByParameters(
			@RequestParam(value = "name", required = false) String jobName,
			@RequestParam(value = "status", required = false) BatchStatus status,
			Pageable pageable, PagedResourcesAssembler<TaskJobExecution> assembler) throws NoSuchJobException, NoSuchJobExecutionException {
		Page<TaskJobExecution> jobExecutions = jobName == null && status == null ? taskJobService.listJobExecutions(pageable)
				: taskJobService.listJobExecutionsForJob(pageable, jobName, status);
		return assembler.toModel(jobExecutions, jobAssembler);
	}

	/**
	 * View the details of a single task execution, specified by id.
	 *
	 * @param id the id of the requested {@link JobExecution}
	 * @return the {@link JobExecution}
	 * @throws NoSuchJobExecutionException if the specified job execution for the id does not
	 *                                     exist.
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public JobExecutionResource view(@PathVariable("id") long id) throws NoSuchJobExecutionException {
		TaskJobExecution jobExecution = taskJobService.getJobExecution(id);
		if (jobExecution == null) {
			throw new NoSuchJobExecutionException(String.format("No Job Execution with id of %d exists", id));
		}
		return jobAssembler.toModel(jobExecution);
	}

	/**
	 * Stop a Job Execution with the given jobExecutionId. Please be aware that you must
	 * provide the request parameter {@code stop=true} in order to invoke this endpoint.
	 *
	 * @param jobExecutionId the executionId of the job execution to stop.
	 * @throws JobExecutionNotRunningException if a stop is requested on a job that is not
	 *                                         running.
	 * @throws NoSuchJobExecutionException     if the job execution id specified does not exist.
	 */
	@RequestMapping(value = {"/{executionId}"}, method = RequestMethod.PUT, params = "stop=true")

	public ResponseEntity<Void> stopJobExecution(
			@PathVariable("executionId") long jobExecutionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		taskJobService.stopJobExecution(jobExecutionId);
		return ResponseEntity.ok().build();
	}

	/**
	 * Restart the Job Execution with the given jobExecutionId. Please be aware that you must
	 * provide the request parameter {@code restart=true} in order to invoke this endpoint.
	 *
	 * @param jobExecutionId the executionId of the job execution to restart
	 * @throws NoSuchJobExecutionException if the job execution for the jobExecutionId
	 *                                     specified does not exist.
	 */
	@RequestMapping(value = {"/{executionId}"}, method = RequestMethod.PUT, params = "restart=true")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<Void> restartJobExecution(
		@PathVariable("executionId") long jobExecutionId,
		@RequestParam(value = "useJsonJobParameters", required = false) Boolean useJsonJobParameters)
		throws NoSuchJobExecutionException {
			taskJobService.restartJobExecution(jobExecutionId, useJsonJobParameters);
			return ResponseEntity.ok().build();
	}

	/**
	 * {@link org.springframework.hateoas.server.RepresentationModelAssembler} implementation that
	 * converts {@link JobExecution}s to {@link JobExecutionResource}s.
	 */
	private static class Assembler extends RepresentationModelAssemblerSupport<TaskJobExecution, JobExecutionResource> {

		private TimeZone timeZone = TimeUtils.getDefaultTimeZone();

		public Assembler() {
			super(JobExecutionController.class, JobExecutionResource.class);
		}

		/**
		 * @param timeZone the timeZone to set
		 */
		@Autowired(required = false)
		@Qualifier("userTimeZone")
		public void setTimeZone(TimeZone timeZone) {
			this.timeZone = timeZone;
		}

		@Override
		public JobExecutionResource toModel(TaskJobExecution taskJobExecution) {
			return instantiateModel(taskJobExecution);
		}

		@Override
		public JobExecutionResource instantiateModel(TaskJobExecution taskJobExecution) {
			JobExecutionResource resource = new JobExecutionResource(taskJobExecution, timeZone);
			try {
				resource.add(linkTo(methodOn(JobExecutionController.class).view(taskJobExecution.getTaskId())).withSelfRel());
				if (taskJobExecution.getJobExecution().isRunning()) {
					resource.add(linkTo(methodOn(JobExecutionController.class).stopJobExecution(taskJobExecution.getJobExecution().getJobId())).withRel("stop"));
				}
				if (!taskJobExecution.getJobExecution().getStatus().equals(BatchStatus.COMPLETED)) {
					// In this case we use null for the useJsonJobParameters parameter, so we use the configured job parameter serialization method specified by dataflow.
					resource.add(linkTo(methodOn(JobExecutionController.class).restartJobExecution(taskJobExecution.getJobExecution().getJobId(), null)).withRel("restart"));
				}
			} catch (NoSuchJobExecutionException | JobExecutionNotRunningException e) {
				throw new RuntimeException(e);
			}
			return resource;
		}
	}
}
