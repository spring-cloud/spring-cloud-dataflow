/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.dataflow.rest.job.support.TimeUtils;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.server.job.TaskJobRepository;
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
 * Controller for operations on {@link org.springframework.batch.core.JobExecution}.
 * This includes obtaining Job execution information from the job explorer.
 *
 * @author Glenn Renfro
 */
@RestController
@RequestMapping("/jobs/executions")
@ExposesResourceFor(JobExecutionResource.class)
public class JobExecutionController {

	private final Assembler jobAssembler = new Assembler();

	private final TaskJobRepository repository;

	/**
	 * Creates a {@code JobExecutionController} that retrieves Job Execution information
	 * from a the {@link JobService}
	 *
	 * @param repository the repository this controller will use for retrieving
	 * job execution information.
	 */
	@Autowired
	public JobExecutionController(TaskJobRepository repository) {
		Assert.notNull(repository, "repository must not be null");
		this.repository = repository;
	}

	/**
	 * Return a page-able list of {@link JobExecutionResource} defined jobs.
	 *
	 * @param pageable  page-able collection of {@code TaskJobExecution}s.
	 * @param assembler for the {@link TaskJobExecution}s
	 * @return a list of Task/Job executions
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<JobExecutionResource> list(Pageable pageable,
			PagedResourcesAssembler<TaskJobExecution> assembler) throws NoSuchJobExecutionException {
		List<TaskJobExecution> jobExecutions = repository.listJobExecutions(pageable);
		Page<TaskJobExecution> page = new PageImpl<>(jobExecutions, pageable, repository.countJobExecutions());
		return assembler.toResource(page, jobAssembler);
	}

	/**
	 * Retrieve all task job executions with the task name specified
	 *
	 * @param jobName   name of the job
	 * @param pageable  page-able collection of {@code TaskJobExecution}s.
	 * @param assembler for the {@link TaskJobExecution}s
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = "name", produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<JobExecutionResource> retrieveJobsByName(
			@RequestParam("name") String jobName, Pageable pageable,
			PagedResourcesAssembler<TaskJobExecution> assembler)
			throws NoSuchJobException {
		List<TaskJobExecution> jobExecutions  = repository.listJobExecutionsForJob(pageable, jobName);
		Page<TaskJobExecution> page = new PageImpl<>(jobExecutions, pageable,
				repository.countJobExecutionsForJob(jobName));
		return assembler.toResource(page, jobAssembler);
	}

	/**
	 * View the details of a single task execution, specified by id.
	 *
	 * @param id the id of the requested {@link JobExecution}
	 * @return the {@link JobExecution}
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = "application/json")
	@ResponseStatus(HttpStatus.OK)
	public JobExecutionResource view(@PathVariable("id") long id) throws NoSuchJobExecutionException {
		TaskJobExecution jobExecution = repository.getJobExecution(id);
		if (jobExecution == null) {
			throw new NoSuchJobExecutionException(String.format("No Job Execution with id of %d exits", id));
		}
		return jobAssembler.toResource(jobExecution);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link JobExecution}s to {@link JobExecutionResource}s.
	 */
	private static class Assembler extends ResourceAssemblerSupport<TaskJobExecution, JobExecutionResource> {

		private TimeZone timeZone = TimeUtils.getDefaultTimeZone();

		/**
		 * @param timeZone the timeZone to set
		 */
		@Autowired(required = false)
		@Qualifier("userTimeZone")
		public void setTimeZone(TimeZone timeZone) {
			this.timeZone = timeZone;
		}

		public Assembler() {
			super(JobExecutionController.class, JobExecutionResource.class);
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
