/*
 * Copyright 2006-2007 the original author or authors.
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
package org.springframework.batch.admin.service;

import java.util.Collection;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.dao.JobExecutionDao;

/**
 * @author Dave Syer
 * 
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
	 * @param start the start index of the instances
	 * @param count the maximum number of instances to return
	 * @return the {@link JobExecution} instances requested
	 */
	List<JobExecution> getJobExecutions(String jobName, int start, int count);

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

}
