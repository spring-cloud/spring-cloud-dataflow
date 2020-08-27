/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.TimeUtils;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionThinResource;
import org.springframework.cloud.dataflow.server.batch.JobService;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for retrieving {@link JobExecution}s where the step executions are
 * not included in the results that are returned.
 *
 * @author Glenn Renfro
 *
 * @since 2.0
 */
@RestController
@RequestMapping("/jobs/thinexecutions")
@ExposesResourceFor(JobExecutionThinResource.class)
public class JobExecutionThinController {

	private final Assembler jobAssembler = new Assembler();

	private final TaskJobService taskJobService;

	/**
	 * Creates a {@code JobExecutionThinController} that retrieves Job Execution information
	 * from a the {@link JobService}
	 *
	 * @param taskJobService the service this controller will use for retrieving job
	 * execution information. Must not be null.
	 */
	@Autowired
	public JobExecutionThinController(TaskJobService taskJobService) {
		Assert.notNull(taskJobService, "taskJobService must not be null");
		this.taskJobService = taskJobService;
	}

	/**
	 * Return a page-able list of {@link JobExecutionThinResource} defined jobs that
	 * do not contain step execution detail.
	 *
	 * @param queryString search query string to filter job names
	 * @param pageable page-able collection of {@code TaskJobExecution}s.
	 * @param assembler for the {@link TaskJobExecution}s
	 * @return a list of Task/Job executions(job executions do not contain step executions.
	 * @throws NoSuchJobExecutionException in the event that a job execution id specified
	 * is not present when looking up stepExecutions for the result.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<JobExecutionThinResource> listJobsOnly(
			@RequestParam(value = "q", required = false) String queryString,
			Pageable pageable, PagedResourcesAssembler<TaskJobExecution> assembler) throws NoSuchJobExecutionException {
		List<TaskJobExecution> jobExecutions;
		if (StringUtils.isEmpty(queryString)) {
			jobExecutions = taskJobService.listJobExecutionsWithStepCount(pageable);
		} else {
			jobExecutions = taskJobService.listJobExecutionsWithStepCount(queryString, pageable);
		}
		Page<TaskJobExecution> page = new PageImpl<>(jobExecutions, pageable, taskJobService.countJobExecutions());
		return assembler.toModel(page, jobAssembler);
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
	public PagedModel<JobExecutionThinResource> retrieveJobsByName(@RequestParam("name") String jobName,
			Pageable pageable, PagedResourcesAssembler<TaskJobExecution> assembler) throws NoSuchJobException {
		List<TaskJobExecution> jobExecutions = taskJobService.listJobExecutionsForJobWithStepCount(pageable, jobName);
		Page<TaskJobExecution> page = new PageImpl<>(jobExecutions, pageable,
				taskJobService.countJobExecutionsForJob(jobName, null));
		return assembler.toModel(page, jobAssembler);
	}



	/**
	 * {@link org.springframework.hateoas.server.ResourceAssembler} implementation that converts
	 * {@link JobExecution}s to {@link JobExecutionThinResource}s.
	 */
	private static class Assembler extends RepresentationModelAssemblerSupport<TaskJobExecution, JobExecutionThinResource> {

		private TimeZone timeZone = TimeUtils.getDefaultTimeZone();

		public Assembler() {
			super(JobExecutionThinController.class, JobExecutionThinResource.class);
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
		public JobExecutionThinResource toModel(TaskJobExecution taskJobExecution) {
			return createModelWithId(taskJobExecution.getJobExecution().getId(), taskJobExecution);
		}

		@Override
		public JobExecutionThinResource instantiateModel(TaskJobExecution taskJobExecution) {
			return new JobExecutionThinResource(taskJobExecution, timeZone);
		}
	}
}
