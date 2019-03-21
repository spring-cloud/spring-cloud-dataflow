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

package org.springframework.cloud.dataflow.rest.job;

import java.util.Collections;
import java.util.List;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.util.Assert;

/**
 * Enumerates the relationship between a {@link JobInstance} and its
 * {@link JobExecution}s.
 *
 * @author Glenn Renfro
 */
public class JobInstanceExecutions {

	private final JobInstance jobInstance;

	private final List<TaskJobExecution> taskJobExecutions;

	public JobInstanceExecutions(JobInstance jobInstance, List<TaskJobExecution> taskJobExecutions) {
		Assert.notNull(jobInstance, "jobInstance must not be null");
		this.jobInstance = jobInstance;
		if (taskJobExecutions == null) {
			this.taskJobExecutions = Collections.emptyList();
		}
		else {
			this.taskJobExecutions = Collections.unmodifiableList(taskJobExecutions);
		}
	}

	/**
	 * @return the job instance for this relationship.
	 */
	public JobInstance getJobInstance() {
		return jobInstance;
	}

	/**
	 * @return a list ot TaskJobExecutions that are associated with the Job Instance for
	 * this relationship.
	 */
	public List<TaskJobExecution> getTaskJobExecutions() {
		return taskJobExecutions;
	}
}
