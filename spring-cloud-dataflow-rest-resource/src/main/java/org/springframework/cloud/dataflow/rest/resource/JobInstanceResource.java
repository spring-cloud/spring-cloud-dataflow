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

package org.springframework.cloud.dataflow.rest.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.cloud.dataflow.rest.job.TaskJobExecution;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * A HATEOAS representation of a JobInstance.
 *
 * @author Glenn Renfro
 */
public class JobInstanceResource extends ResourceSupport {

	private final String jobName;

	private final long jobInstanceId;

	private List<JobExecutionResource> jobExecutions;

	public JobInstanceResource(String jobName, long jobInstanceId, List<TaskJobExecution> taskJobExecutions) {
		Assert.hasText(jobName, "jobName must not be empty nor null");
		this.jobName = jobName;
		this.jobInstanceId = jobInstanceId;
		if(taskJobExecutions == null){
			this.jobExecutions = Collections.emptyList();
		}else {
			this.jobExecutions = new ArrayList<>();
			for(TaskJobExecution taskJobExecution: taskJobExecutions){
				this.jobExecutions.add(new JobExecutionResource(jobName, taskJobExecution));
			}
			this.jobExecutions = Collections.unmodifiableList(this.jobExecutions);
		}
	}

	public String getJobName() {
		return jobName;
	}

	public long getJobInstanceId() {
		return jobInstanceId;
	}

	public List<JobExecutionResource> getJobExecutions() {
		return jobExecutions;
	}
}
