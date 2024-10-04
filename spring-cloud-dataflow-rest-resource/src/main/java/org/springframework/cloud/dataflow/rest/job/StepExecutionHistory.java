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

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.batch.core.StepExecution;

/**
 * Stores the cumulative information for a specific {@link StepExecution}'s history.
 * }
 * @author Glenn Renfro
 */
public class StepExecutionHistory {
	private final String stepName;

	private int count = 0;

	private CumulativeHistory commitCount = new CumulativeHistory();

	private CumulativeHistory rollbackCount = new CumulativeHistory();

	private CumulativeHistory readCount = new CumulativeHistory();

	private CumulativeHistory writeCount = new CumulativeHistory();

	private CumulativeHistory filterCount = new CumulativeHistory();

	private CumulativeHistory readSkipCount = new CumulativeHistory();

	private CumulativeHistory writeSkipCount = new CumulativeHistory();

	private CumulativeHistory processSkipCount = new CumulativeHistory();

	private CumulativeHistory duration = new CumulativeHistory();

	private CumulativeHistory durationPerRead = new CumulativeHistory();

	public StepExecutionHistory(String stepName) {
		this.stepName = stepName;
	}

	public void append(StepExecution stepExecution) {
		if (stepExecution.getEndTime() == null) {
			// ignore unfinished executions
			return;
		}
		LocalDateTime startTime = stepExecution.getStartTime();
		LocalDateTime endTime = stepExecution.getEndTime();
		long time = Duration.between(startTime, endTime).toMillis();
		duration.append(time);
		if (stepExecution.getReadCount() > 0) {
			durationPerRead.append(time / stepExecution.getReadCount());
		}
		count++;
		commitCount.append(stepExecution.getCommitCount());
		rollbackCount.append(stepExecution.getRollbackCount());
		readCount.append(stepExecution.getReadCount());
		writeCount.append(stepExecution.getWriteCount());
		filterCount.append(stepExecution.getFilterCount());
		readSkipCount.append(stepExecution.getReadSkipCount());
		writeSkipCount.append(stepExecution.getWriteSkipCount());
		processSkipCount.append(stepExecution.getProcessSkipCount());
	}

	public String getStepName() {
		return stepName;
	}

	/**
	 * Returns the number of {@link StepExecution}s are being used for history calculations.
	 *
	 * The id of an existing step execution for a specific job execution (required)
	 * @return the number of {@link StepExecution}s.
	 */
	public int getCount() {
		return count;
	}

	@Deprecated
	public CumulativeHistory getCommitCount() {
		return commitCount;
	}

	@Deprecated
	public CumulativeHistory getRollbackCount() {
		return rollbackCount;
	}

	public CumulativeHistory getReadCount() {
		return readCount;
	}

	@Deprecated
	public CumulativeHistory getWriteCount() {
		return writeCount;
	}

	@Deprecated
	public CumulativeHistory getFilterCount() {
		return filterCount;
	}

	@Deprecated
	public CumulativeHistory getReadSkipCount() {
		return readSkipCount;
	}

	@Deprecated
	public CumulativeHistory getWriteSkipCount() {
		return writeSkipCount;
	}

	@Deprecated
	public CumulativeHistory getProcessSkipCount() {
		return processSkipCount;
	}

	/**
	 * Stores the cumulative history for a specified {@link StepExecution}'s duration.
	 * @return {@link CumulativeHistory} for the duration of a specified {@link StepExecution}.
	 */
	public CumulativeHistory getDuration() {
		return duration;
	}

	@Deprecated
	public CumulativeHistory getDurationPerRead() {
		return durationPerRead;
	}

}
