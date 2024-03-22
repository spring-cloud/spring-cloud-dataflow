/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.cloud.dataflow.server.batch;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.dao.JobExecutionDao;

/**
 * @author Dave Syer
 * @author Corneil du Plessis
 */
public interface SearchableJobExecutionDao extends JobExecutionDao {

	/**
	 * @return the total number of {@link JobExecution} instances
	 */
	int countJobExecutions();

	/**
	 * Get the {@link JobExecution JobExecutions} for a specific job name in
	 * reverse order of creation (so normally of execution).
	 *
	 * @param jobName the name of the job
	 * @param status the status of the job
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecution} instances requested
	 */
	List<JobExecution> getJobExecutions(String jobName, BatchStatus status, int start, int count);

    /**
     * Get the {@link JobExecution JobExecutions} for a specific job name in
     * reverse order of creation (so normally of execution).
     *
     * @param jobName the name of the job
     * @param start the start index of the instances
     * @param count the maximum number of instances to return
     * @return the {@link JobExecution} instances requested
     */
    List<JobExecution> getJobExecutions(String jobName, int start, int count);

	/**
	 * Get the {@link JobExecution JobExecutions} for a specific status in
	 * reverse order of creation (so normally of execution).
	 *
	 * @param start the start index of the instances
	 * @param status the status of the job
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecution} instances requested
	 */
	List<JobExecution> getJobExecutions(BatchStatus status, int start, int count);

	/**
	 * Get the {@link JobExecutionWithStepCount JobExecutions} for a specific job name in
	 * reverse order of creation (so normally of execution).
	 *
	 * @param jobName the name of the job
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecutionWithStepCount} instances requested
	 */
	List<JobExecutionWithStepCount> getJobExecutionsWithStepCount(String jobName, int start, int count);

	/**
	 * Get the {@link JobExecution JobExecutions} in reverse order of creation
	 * (so normally of execution).
	 *
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecution} instances requested
	 */
	List<JobExecution> getJobExecutions(int start, int count);

	/**
	 * Get the {@link JobExecutionWithStepCount JobExecutions} in reverse order of creation
	 * (so normally of execution) without StepExecution.
	 *
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecutionWithStepCount} instances requested
	 */
	List<JobExecutionWithStepCount> getJobExecutionsWithStepCount(int start, int count);

	/**
	 * @param ids the set of task execution ids.
	 * @return Map with the TaskExecution id as the key and the set of job execution ids as values.
	 */
	Map<Long, Set<Long>> getJobExecutionsByTaskIds(Collection<Long> ids);
	/**
	 * Gets count of job executions.
	 *
	 * @param jobName the name of a job
	 * @return the number of {@link JobExecution JobExecutions} belonging to
	 * this job
	 */
	int countJobExecutions(String jobName);

	/**
	 * Find all the running executions (status less than STOPPING).
	 *
	 * @return all the {@link JobExecution} instances that are currently running
	 */
	Collection<JobExecution> getRunningJobExecutions();

	/**
	 * Gets count of job executions.
	 *
	 * @param status the job status
	 * @return the number of {@link JobExecution JobExecutions} belonging to
	 * this job
	 */
	int countJobExecutions(BatchStatus status);


	/**
	 * Gets count of job executions.
	 *
	 * @param jobName the name of a job
	 * @param status the job status
	 * @return the number of {@link JobExecution JobExecutions} belonging to
	 * this job
	 */
	int countJobExecutions(String jobName, BatchStatus status);

	/**
	 * Get the {@link JobExecutionWithStepCount JobExecutions} for a specific date range in
	 * reverse order of creation (so normally of execution).
	 *
	 * @param fromDate the date which start date must be greater than.
	 * @param toDate the date which start date must be less than.
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecutionWithStepCount} instances requested
	 */
	List<JobExecutionWithStepCount> getJobExecutionsWithStepCount(Date fromDate, Date toDate, int start, int count);

	/**
	 * Get the {@link JobExecutionWithStepCount JobExecutions} for a specific job instance id in
	 * reverse order of creation (so normally of execution).
	 *
	 * @param jobInstanceId the job instance id associated with the execution.
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecutionWithStepCount} instances requested
	 */
	List<JobExecutionWithStepCount> getJobExecutionsWithStepCountFilteredByJobInstanceId(int jobInstanceId, int start, int count);

	/**
	 * Get the {@link JobExecutionWithStepCount JobExecutions} for a specific task execution id in
	 * reverse order of creation (so normally of execution).
	 *
	 * @param taskExecutionId the task execution id associated with the execution.
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecutionWithStepCount} instances requested
	 */
	List<JobExecutionWithStepCount> getJobExecutionsWithStepCountFilteredByTaskExecutionId(int taskExecutionId, int start, int count);
}
