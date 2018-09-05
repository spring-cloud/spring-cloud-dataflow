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

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.StepExecutionDao;

/**
 * Extension of the {@link StepExecutionDao} interface to add additional search
 * features.
 * 
 * @author Dave Syer
 * 
 */
public interface SearchableStepExecutionDao extends StepExecutionDao {

	/**
	 * Find all the names of steps that have been executed as part of this job.
	 * Implementations should remove step names matching the pattern provided.
	 * 
	 * @param jobName
	 *            the name of the job to search for
	 * @param excludesPattern
	 *            a pattern for step names to exclude
	 * @return the names of step executions from job executions in the
	 *         repository
	 */
	Collection<String> findStepNamesForJobExecution(String jobName,
			String excludesPattern);

	/**
	 * Find all the step executions for a given step name, or step name pattern
	 * (with wildcards specified as '*') sorted in descending order of id.
	 * @param jobName the job name or pattern
	 * @param stepName
	 *            the step name or pattern
	 * @param start
	 *            the start index of the step executions to return
	 * @param count
	 *            the maximum number of step executions to return
	 * 
	 * @return a collection of step executions
	 */
	Collection<StepExecution> findStepExecutions(String jobName, String stepName,
			int start, int count);
	
	/**
	 * Count all the step executions for a given step name pattern.
	 * @param jobName the job name pattern
	 * @param stepName the step name pattern.
	 * 
	 * @return the total number of step executions in the repository matching the pattern
	 */
	int countStepExecutions(String jobName, String stepName);
}
