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

import java.util.ArrayList;
import java.util.List;

import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecution;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
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
 * Controller for operations on {@link JobInstance}.
 * This includes obtaining Job Instance information from the job service.
 *
 * @author Glenn Renfro
 */
@RestController
@RequestMapping("/jobs/instances")
@ExposesResourceFor(JobInstanceResource.class)
public class JobInstanceController {

	private final Assembler jobAssembler = new Assembler();

	private final TaskJobRepository repository;

	/**
	 * Creates a {@code JobInstanceController} that retrieves Job Instance information.
	 *
	 * @param repository the {@link TaskJobRepository} used for retrieving batch instance data.
	 */
	@Autowired
	public JobInstanceController(TaskJobRepository repository) {
		Assert.notNull(repository, "repository must not be null");
		this.repository = repository;
	}

	/**
	 * Return a page-able list of {@link JobInstanceResource} defined jobs.
	 *
	 * @param pageable  page-able collection of {@link JobInstance}s.
	 * @param assembler for the {@link JobInstance}s
	 * @return a list of Job Instance
	 */
	@RequestMapping(value = "", method = RequestMethod.GET, params = "name")
	@ResponseStatus(HttpStatus.OK)
	public PagedResources<JobInstanceResource> list(@RequestParam("name") String jobName,
			Pageable pageable, PagedResourcesAssembler<JobInstanceExecution> assembler)
			throws NoSuchJobException {
		List<JobInstanceExecution> jobInstances;
		Page page;
		try{
			jobInstances = repository.listTaskJobInstancesForJobName(pageable, jobName);
			page = new PageImpl<>(jobInstances, pageable, repository.countJobInstances(jobName));
		}
		catch (NoSuchJobException e){
				page = new PageImpl<>(new ArrayList<JobInstanceExecution>());
		}
		return assembler.toResource(page, jobAssembler);
	}

	/**
	 * View the details of a single task instance, specified by id.
	 *
	 * @param id the id of the requested {@link JobInstance}
	 * @return the {@link JobInstance}
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	public JobInstanceResource view(@PathVariable("id") long id)
			throws NoSuchJobInstanceException, NoSuchJobException {
		JobInstanceExecution jobInstance = repository.getJobInstance(id);
		return jobAssembler.toResource(jobInstance);
	}

	/**
	 * {@link org.springframework.hateoas.ResourceAssembler} implementation
	 * that converts {@link JobInstance}s to {@link JobInstanceResource}s.
	 */
	private static class Assembler extends ResourceAssemblerSupport<JobInstanceExecution, JobInstanceResource> {

		public Assembler() {
			super(JobInstanceController.class, JobInstanceResource.class);
		}

		@Override
		public JobInstanceResource toResource(JobInstanceExecution jobInstance) {
			return createResourceWithId(jobInstance.getJobInstance().getInstanceId(), jobInstance);
		}

		@Override
		public JobInstanceResource instantiateResource(JobInstanceExecution jobInstance) {
			return new JobInstanceResource(jobInstance.getJobInstance().getJobName(),
							jobInstance.getJobInstance().getInstanceId(),jobInstance.getTaskJobExecutions());
		}
	}
}
