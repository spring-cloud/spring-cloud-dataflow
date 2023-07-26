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

import org.springframework.cloud.dataflow.rest.resource.JobExecutionResource;
import org.springframework.cloud.dataflow.rest.resource.JobExecutionThinResource;
import org.springframework.cloud.dataflow.rest.resource.JobInstanceResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionProgressInfoResource;
import org.springframework.cloud.dataflow.rest.resource.StepExecutionResource;
import org.springframework.hateoas.PagedModel;

/**
 * Interface defining operations available for jobs.
 *
 * @author Glenn Renfro
 */
public interface JobOperations {

	/**
	 * @return the list job executions known to the system.
	 */
	PagedModel<JobExecutionResource> executionList();

	/**
	 * Restarts a job by id
	 *
	 * @param id job execution id
	 */
	void executionRestart(long id, String schemaTarget);

	/**
	 * @return the list job executions without step executions known to the system.
	 */
	PagedModel<JobExecutionThinResource> executionThinList();

	/**
	 * @param jobName the name of the job
	 * @return the list job instances for the specified jobName.
	 */
	PagedModel<JobInstanceResource> instanceList(String jobName);

	/**
	 * List job executions without step executions known to the system filtered by job name.
	 *
	 * @param jobName of the executions.
	 * @return the list of job executions
	 */
	PagedModel<JobExecutionThinResource> executionThinListByJobName(String jobName);

	/**
	 * List job executions known to the system filtered by job name.
	 *
	 * @param jobName of the executions.
	 * @return the list of job executions
	 */
	PagedModel<JobExecutionResource> executionListByJobName(String jobName);

	/**
	 * Return the {@link JobExecutionResource} for the id specified.
	 *
	 * @param id identifier of the job execution
	 * @return {@link JobExecutionResource}
	 */
	JobExecutionResource jobExecution(long id, String schemaTarget);

	/**
	 * Return the {@link JobInstanceResource} for the id specified.
	 *
	 * @param id identifier of the job instasnce
	 * @return {@link JobInstanceResource}
	 */
	JobInstanceResource jobInstance(long id, String schemaTarget);

	/**
	 * List step executions known for a specific job execution id.
	 *
	 * @param jobExecutionId the id of the job execution.
	 * @return the paged list of step executions
	 */
	PagedModel<StepExecutionResource> stepExecutionList(long jobExecutionId, String schemaTarget);

	/**
	 * Return StepExecutionProgressInfoResource for a specific job execution id and step
	 * execution Id.
	 *
	 * @param jobExecutionId the id of the job execution for the step to be returned.
	 * @param stepExecutionId the id step execution to be returned.
	 * @return the step execution progress info
	 */
	StepExecutionProgressInfoResource stepExecutionProgress(long jobExecutionId, long stepExecutionId, String schemaTarget);

}
