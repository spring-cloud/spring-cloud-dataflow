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

package org.springframework.cloud.dataflow.server.job;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.batch.admin.service.JobService;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.launch.NoSuchJobInstanceException;
import org.springframework.cloud.dataflow.rest.job.JobInstanceExecution;
import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.cloud.task.repository.TaskExplorer;
import org.springframework.data.domain.Pageable;
import org.springframework.util.Assert;

/**
 * Repository that retrieves Tasks and JobExecutions/Instances and the associations
 * between them.
 *
 * @author Glenn Renfro.
 */
public class TaskJobRepository {

	private TaskExplorer taskExplorer;

	private JobService jobService;


	public TaskJobRepository(JobService jobService, TaskExplorer taskExplorer){
		Assert.notNull(jobService, "jobService must not be null");
		Assert.notNull(taskExplorer, "taskExplorer must not be null");
		this.jobService = jobService;
		this.taskExplorer = taskExplorer;
	}
	/**
	 * Retrieves Pageable list of {@link JobExecution}s from the JobRepository and matches
	 * the data with a task id.
	 * @param pageable enumerates the data to be returned.
	 * @return List containing {@link TaskJobExecution}s.
	 */
	public List<TaskJobExecution> listJobExecutions(Pageable pageable){
		return getTaskJobExecutionsForList(
				jobService.listJobExecutions(pageable.getOffset(), pageable.getPageSize()));
	}

	/**
	 * Retrieves Pageable list of {@link JobExecution} from the JobRepository with a
	 * specific jobName and matches the data with a task id.
	 * @param pageable enumerates the data to be returned.
	 * @param jobName the name of the job for which to search.
	 * @return List containing {@link TaskJobExecution}s.
	 */
	public List<TaskJobExecution> listJobExecutionsForJob (Pageable pageable,
			String jobName) throws NoSuchJobException{
		return getTaskJobExecutionsForList(
				jobService.listJobExecutionsForJob(jobName,pageable.getOffset(),
						pageable.getPageSize()));
	}

	/**
	 * Retrieves a JobExecution from the JobRepository and matches it with a task id.
	 * @param id the id of the {@link JobExecution}
	 * @return the {@link TaskJobExecution}s associated with the id.
	 */
	public TaskJobExecution getJobExecution(long id) throws NoSuchJobExecutionException{
		JobExecution jobExecution = jobService.getJobExecution(id);
		jobExecution.setStartTime(new Date());
		return getTaskJobExecution(jobExecution);
	}

	/**
	 * Retrieves Pageable list of {@link JobInstanceExecution} from the JobRepository with a
	 * specific jobName and matches the data with the associated JobExecutions.
	 * @param pageable enumerates the data to be returned.
	 * @param jobName the name of the job for which to search.
	 * @return List containing {@link JobInstanceExecution}s.
	 */
	public List<JobInstanceExecution> listTaskJobInstancesForJobName(Pageable pageable,
			String jobName) throws NoSuchJobException{
		List<JobInstanceExecution> taskJobInstances = new ArrayList<>();
		for(JobInstance jobInstance : jobService.listJobInstances(
				jobName, pageable.getOffset(), pageable.getPageSize())){
			taskJobInstances.add(getJobInstanceExecution(jobInstance));
		}
		return taskJobInstances;
	}

	/**
	 * Retrieves a {@link JobInstance} from the JobRepository and matches it with the associated
	 * {@link JobExecution}s.
	 * @param id the id of the {@link JobInstance}
	 * @return the {@link JobInstanceExecution}s associated with the id.
	 */
	public JobInstanceExecution getJobInstance(long id)
			throws NoSuchJobInstanceException, NoSuchJobException {
		return getJobInstanceExecution(jobService.getJobInstance(id));
	}

	/**
	 * Retrieves the total number of job instances for a job name.
	 * @param jobName the name of the job instance.
	 */
	public int countJobInstances(String jobName) throws NoSuchJobException{
		return jobService.countJobInstances(jobName);
	}

	/**
	 * Retrieves the total number of the job executions.
	 */
	public int countJobExecutions(){
		return jobService.countJobExecutions();
	}

	/**
	 * Retrieves the total number {@link JobExecution} that match a specific job name.
	 * @param jobName the job name to search.
	 * @return the number of {@link JobExecution}s that match the job name.
	 * @throws NoSuchJobException
	 */
	public int countJobExecutionsForJob(String jobName) throws NoSuchJobException{
		return jobService.countJobExecutionsForJob(jobName);
	}

	private List<TaskJobExecution> getTaskJobExecutionsForList(Collection<JobExecution> jobExecutions){
		List<TaskJobExecution> taskJobExecutions = new ArrayList<>();
		for(JobExecution jobExecution : jobExecutions){
			taskJobExecutions.add(getTaskJobExecution(jobExecution));
		}
		return taskJobExecutions;
	}

	private TaskJobExecution getTaskJobExecution(JobExecution jobExecution){
		return new TaskJobExecution(
				taskExplorer.getTaskExecutionIdByJobExecutionId(jobExecution.getJobId()),
				jobExecution);
	}

	private JobInstanceExecution getJobInstanceExecution(JobInstance jobInstance)
			throws NoSuchJobException {
		List<JobExecution> jobExecutions = new ArrayList<>(
				jobService.getJobExecutionsForJobInstance(
						jobInstance.getJobName(), jobInstance.getInstanceId()));
		return new JobInstanceExecution(jobInstance,
				getTaskJobExecutionsForList(jobExecutions));
	}

}
