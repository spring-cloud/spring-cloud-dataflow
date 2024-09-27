/*
 * Copyright 2016-2017 the original author or authors.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecutions;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.TimeUtils;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.server.service.TaskJobService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.server.ExposesResourceFor;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.http.HttpStatus;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for operations on {@link JobInstance}. This includes obtaining Job Instance
 * information from the job service.
 *
 * @author Glenn Renfro
 * @author Gunnar Hillert
 */
@RestController
@RequestMapping("/jobs/instances")
@ExposesResourceFor(JobInstanceResource.class)
public class JobInstanceController {

	private final Assembler jobAssembler = new Assembler();

	private final TaskJobService taskJobService;

	/**
	 * Creates a {@code JobInstanceController} that retrieves Job Instance information.
	 *
	 * @param taskJobService the {@link TaskJobService} used for retrieving batch instance
	 *                       data.
	 */
	@Autowired
	public JobInstanceController(TaskJobService taskJobService) {
		Assert.notNull(taskJobService, "taskJobService must not be null");
		this.taskJobService = taskJobService;
	}

	/**
	 * Return a page-able list of {@link JobInstanceResource} defined jobs.
	 *
	 * @param jobName   the name of the job
	 * @param pageable  page-able collection of {@link JobInstance}s.
	 * @param assembler for the {@link JobInstance}s
	 * @return a list of Job Instance
	 * @throws NoSuchJobException if the job for jobName specified does not exist.
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = "name")
	@ResponseStatus(HttpStatus.OK)
	public PagedModel<JobInstanceResource> list(
			@RequestParam("name") String jobName,
			Pageable pageable,
			PagedResourcesAssembler<JobInstanceExecutions> assembler) throws NoSuchJobException {
		Page<JobInstanceExecutions> jobInstances = taskJobService.listTaskJobInstancesForJobName(pageable, jobName);
		return assembler.toModel(jobInstances, jobAssembler);
	}

	/**
	 * View the details of a single task instance, specified by id.
	 *
	 * @param id the id of the requested {@link JobInstance}
	 * @return the {@link JobInstance}
	 * @throws NoSuchJobInstanceException if job instance for the id does not exist.
	 * @throws NoSuchJobException         if the job for the job instance does not exist.
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public JobInstanceResource view(
			@PathVariable("id") long id
	) throws NoSuchJobInstanceException, NoSuchJobException {
		JobInstanceExecutions jobInstance = taskJobService.getJobInstance(id);
		if (jobInstance == null) {
			throw new NoSuchJobInstanceException(String.format("No job instance for id '%d'", id));
		}
		return jobAssembler.toModel(jobInstance);
	}

	/**
	 * {@link RepresentationModelAssemblerSupport} implementation that converts
	 * {@link JobInstance}s to {@link JobInstanceResource}s.
	 */
	private static class Assembler extends RepresentationModelAssemblerSupport<JobInstanceExecutions, JobInstanceResource> {

		private TimeZone timeZone = TimeUtils.getDefaultTimeZone();

		public Assembler() {
			super(JobInstanceController.class, JobInstanceResource.class);
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
		public JobInstanceResource toModel(JobInstanceExecutions jobInstance) {
			return createModelWithId(jobInstance.getJobInstance().getInstanceId(), jobInstance);
		}

		@Override
		public JobInstanceResource instantiateModel(JobInstanceExecutions jobInstance) {
			List<JobExecutionResource> jobExecutions = new ArrayList<>();
			for (TaskJobExecution taskJobExecution : jobInstance.getTaskJobExecutions()) {
				jobExecutions.add(new JobExecutionResource(taskJobExecution, timeZone));
			}
			jobExecutions = Collections.unmodifiableList(jobExecutions);
			return new JobInstanceResource(jobInstance.getJobInstance().getJobName(),
					jobInstance.getJobInstance().getInstanceId(), jobExecutions);
		}
	}
}
