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

import java.util.List;
import java.util.TimeZone;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.TimeUtils;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.ExposesResourceFor;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.mvc.ResourceAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link org.springframework.batch.core.JobExecution}. This
 * includes obtaining Job execution information from the job explorer.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/jobs/executions")
@ExposesResourceFor(JobExecutionResource.class)
public class JobExecutionController {

	private final Assembler jobAssembler = new Assembler();

	private final TaskJobService taskJobService;

	/**
	 * Creates a {@code JobExecutionController} that retrieves Job Execution information
	 * from a the {@link JobService}
	 *
	 * @param taskJobService the service this controller will use for retrieving job
	 * execution information. Must not be null.
	 */
	@Autowired
	public JobExecutionController(TaskJobService taskJobService) {
		Assert.notNull(taskJobService, "taskJobService must not be null");
		this.taskJobService = taskJobService;
	}

	/**
	 * Return a page-able list of {@link JobExecutionResource} defined jobs.
	 *
	 * @param pageable page-able collection of {@code TaskJobExecution}s.
	 * @param assembler for the {@link TaskJobExecution}s
	 * @return a list of Task/Job executions
	 * @throws NoSuchJobExecutionException in the event that a job execution id specified
	 * is not present when looking up stepExecutions for the result.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<JobExecutionResource> list(Pageable pageable,
			PagedResourcesAssembler<TaskJobExecution> assembler) throws NoSuchJobExecutionException {
		List<TaskJobExecution> jobExecutions = taskJobService.listJobExecutions(pageable);
		Page<TaskJobExecution> page = new PageImpl<>(jobExecutions, pageable, taskJobService.countJobExecutions());
		return assembler.toResource(page, jobAssembler);
	}

	/**
	 * Retrieve all task job executions with the task name specified
	 *
	 * @param jobName name of the job
	 * @param pageable page-able collection of {@code TaskJobExecution}s.
	 * @param assembler for the {@link TaskJobExecution}s
	 * @return list task/job executions with the specified jobName.
	 * @throws NoSuchJobException if the job with the given name does not exist.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = "name", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<JobExecutionResource> retrieveJobsByName(@RequestParam("name") String jobName,
			Pageable pageable, PagedResourcesAssembler<TaskJobExecution> assembler) throws NoSuchJobException {
		List<TaskJobExecution> jobExecutions = taskJobService.listJobExecutionsForJob(pageable, jobName);
		Page<TaskJobExecution> page = new PageImpl<>(jobExecutions, pageable,
				taskJobService.countJobExecutionsForJob(jobName));
		return assembler.toResource(page, jobAssembler);
	}

	/**
	 * View the details of a single task execution, specified by id.
	 *
	 * @param id the id of the requested {@link JobExecution}
	 * @return the {@link JobExecution}
	 * @throws NoSuchJobExecutionException if the specified job execution for the id does
	 * not exist.
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public JobExecutionResource view(@PathVariable("id") long id) throws NoSuchJobExecutionException {
		TaskJobExecution jobExecution = taskJobService.getJobExecution(id);
		if (jobExecution == null) {
			throw new NoSuchJobExecutionException(String.format("No Job Execution with id of %d exits", id));
		}
		return jobAssembler.toResource(jobExecution);
	}

	/**
	 * Stop a Job Execution with the given jobExecutionId. Please be aware that you must
	 * provide the request parameter {@code stop=true} in order to invoke this endpoint.
	 *
	 * @param jobExecutionId the executionId of the job execution to stop.
	 * @throws JobExecutionNotRunningException if a stop is requested on a job that is not
	 * running.
	 * @throws NoSuchJobExecutionException if the job execution id specified does not
	 * exist.
	 */
	@RequestMapping(value = { "/{executionId}" }, method = RequestMethod.PUT, params = "stop=true")
	@ResponseStatus(HttpStatus.OK)
	public void stopJobExecution(@PathVariable("executionId") long jobExecutionId)
			throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		taskJobService.stopJobExecution(jobExecutionId);
	}

	/**
	 * Restart the Job Execution with the given jobExecutionId. Please be aware that you
	 * must provide the request parameter {@code restart=true} in order to invoke this
	 * endpoint.
	 *
	 * @param jobExecutionId the executionId of the job execution to restart
	 * @throws NoSuchJobExecutionException if the job execution for the jobExecutionId
	 * specified does not exist.
	 */
	@RequestMapping(value = { "/{executionId}" }, method = RequestMethod.PUT, params = "restart=true")
	@ResponseStatus(HttpStatus.OK)
	public void restartJobExecution(@PathVariable("executionId") long jobExecutionId)
			throws NoSuchJobExecutionException {
		taskJobService.restartJobExecution(jobExecutionId);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation that converts
	 * {@link JobExecution}s to {@link JobExecutionResource}s.
	 */
	private static class Assembler extends ResourceAssemblerSupport<TaskJobExecution, JobExecutionResource> {

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
		public JobExecutionResource toResource(TaskJobExecution taskJobExecution) {
			return createResourceWithId(taskJobExecution.getJobExecution().getId(), taskJobExecution);
		}

		@Override
		public JobExecutionResource instantiateResource(TaskJobExecution taskJobExecution) {
			return new JobExecutionResource(taskJobExecution, timeZone);
		}
	}
}
