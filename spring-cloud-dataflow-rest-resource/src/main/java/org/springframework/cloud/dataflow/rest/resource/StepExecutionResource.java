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

import java.util.Date;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * @author Glenn Renfro
 */
public class StepExecutionResource extends ResourceSupport{

	private final long jobExecutionId;

	private final long stepExecutionId;

	private final String stepName;

	private Date startTime;

	private Date endTime;

	private BatchStatus status;

	private Date lastUpdated;

	private int readCount;

	private int writeCount;

	private int filterCount;

	private int readSkipCount;

	private int writeSkipCount;

	private int processSkipCount;

	private int commitCount;

	private int rollbackCount;

	private ExitStatus exitStatus;

	private String exitDescription;

	public StepExecutionResource(StepExecution stepExecution) {
		Assert.notNull(stepExecution, "stepExecution must not be null");
		this.stepExecutionId = stepExecution.getId();
		this.jobExecutionId = stepExecution.getJobExecutionId();
		this.stepName = stepExecution.getStepName();
		this.startTime = stepExecution.getStartTime();
		this.endTime = stepExecution.getEndTime();
		this.status = stepExecution.getStatus();
		this.lastUpdated = stepExecution.getLastUpdated();
		this.readCount = stepExecution.getReadCount();
		this.writeCount = stepExecution.getWriteCount();
		this.filterCount = stepExecution.getFilterCount();
		this.readSkipCount = stepExecution.getReadSkipCount();
		this.writeSkipCount = stepExecution.getWriteSkipCount();
		this. processSkipCount = stepExecution.getProcessSkipCount();
		this. commitCount = stepExecution.getCommitCount();
		this.rollbackCount = stepExecution.getRollbackCount();
		this.exitStatus = stepExecution.getExitStatus();
		this. exitDescription = stepExecution.getExitStatus().getExitDescription();
	}

	/**
	 * @return The stepExecutionId
	 */
	public long getStepExecutionId() {
		return stepExecutionId;
	}

	public String getStepName() {
		return stepName;
	}

	public long getJobExecutionId(){
		return jobExecutionId;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public BatchStatus getStatus() {
		return status;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public int getReadCount() {
		return readCount;
	}

	public int getWriteCount() {
		return writeCount;
	}

	public int getFilterCount() {
		return filterCount;
	}

	public int getReadSkipCount() {
		return readSkipCount;
	}

	public int getWriteSkipCount() {
		return writeSkipCount;
	}

	public int getProcessSkipCount() {
		return processSkipCount;
	}

	public int getCommitCount() {
		return commitCount;
	}

	public int getRollbackCount() {
		return rollbackCount;
	}

	public ExitStatus getExitStatus() {
		return exitStatus;
	}

	public String getExitDescription() {
		return exitDescription;
	}
}
