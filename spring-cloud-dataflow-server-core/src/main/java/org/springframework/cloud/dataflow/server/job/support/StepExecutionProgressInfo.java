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

package org.springframework.cloud.dataflow.server.job.support;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.springframework.batch.core.StepExecution;
import org.springframework.cloud.dataflow.rest.job.CumulativeHistory;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.util.StringUtils;

/**
 * Represents Batch job step execution progress info.
 *
 * @author Dave Syer
 * @author Ilayaperumal Gopinathan
 * @author Gary Russell
 * @since 1.0
 */
public class StepExecutionProgressInfo {

	private final StepExecution stepExecution;

	private final StepExecutionHistory stepExecutionHistory;

	private double duration = 0;

	private double percentageComplete = 0.5;

	private boolean isFinished = false;

	private PercentCompleteBasis percentCompleteBasis = PercentCompleteBasis.UNKNOWN;

	public StepExecutionProgressInfo(StepExecution stepExecution, StepExecutionHistory stepExecutionHistory) {
		this.stepExecution = stepExecution;
		this.stepExecutionHistory = stepExecutionHistory;
		LocalDateTime startTime = stepExecution.getStartTime();
		LocalDateTime endTime = stepExecution.getEndTime();
		if (endTime == null) {
			endTime = LocalDateTime.now();
		}
		else {
			isFinished = true;
		}
		if (startTime == null) {
			startTime = LocalDateTime.now();
		}
		duration = Duration.between(startTime, endTime).get(ChronoUnit.NANOS);
		percentageComplete = calculatePercentageComplete();
	}

	public MessageSourceResolvable getEstimatedPercentCompleteMessage() {

		String defaultMessage = String.format(
				"This execution is estimated to be %.0f%% complete after %.0f ms based on %s", percentageComplete * 100,
				duration, percentCompleteBasis.getMessage().getDefaultMessage());

		DefaultMessageSourceResolvable message = new DefaultMessageSourceResolvable(
				new String[] { "step.execution.estimated.progress" },
				new Object[] { percentageComplete, duration, percentCompleteBasis.getMessage() }, defaultMessage);

		return message;

	}

	public boolean isFinished() {
		return isFinished;
	}

	public double getDuration() {
		return duration;
	}

	public double getEstimatedPercentComplete() {
		return percentageComplete;
	}

	private double calculatePercentageComplete() {

		if (isFinished) {
			percentCompleteBasis = PercentCompleteBasis.ENDTIME;
			return 1;
		}

		if (stepExecutionHistory.getCount() == 0) {
			percentCompleteBasis = PercentCompleteBasis.NOHISTORY;
			return 0.5;
		}

		CumulativeHistory readHistory = stepExecutionHistory.getReadCount();

		double result = 0.0;
		if (readHistory.getMean() == 0) {
			percentCompleteBasis = PercentCompleteBasis.DURATION;
			result = getDurationBasedEstimate();
		}
		else {
			percentCompleteBasis = PercentCompleteBasis.READCOUNT;
			result = stepExecution.getReadCount() / readHistory.getMean();
		}
		return result;
	}

	private double getDurationBasedEstimate() {

		CumulativeHistory durationHistory = stepExecutionHistory.getDuration();
		if (durationHistory.getMean() == 0) {
			percentCompleteBasis = PercentCompleteBasis.NOINFORMATION;
			return 0.5;
		}
		return duration / durationHistory.getMean();

	}

	public Long getStepExecutionId() {
		return stepExecution.getId();
	}

	public StepExecution getStepExecution() {
		return stepExecution;
	}

	public StepExecutionHistory getStepExecutionHistory() {
		return stepExecutionHistory;
	}

	private enum PercentCompleteBasis {

		UNKNOWN("unknown"),
		NOINFORMATION("percent.no.information,no.information", "no information"),
		ENDTIME("percent.end.time,end.time", "end time (already finished)"),
		DURATION("percent.duration,duration", "extrapolated duration"),
		READCOUNT("percent.read.count,read.count", "extrapolated read count"),
		NOHISTORY("percent.no.history,no.history", "no history");

		private final String[] codes;

		private final String message;

		private PercentCompleteBasis(String code) {
			this(code, code);
		}

		private PercentCompleteBasis(String codes, String message) {
			this(StringUtils.commaDelimitedListToStringArray(codes), message);
		}

		private PercentCompleteBasis(String[] code, String message) {
			this.codes = Arrays.copyOf(code, code.length);
			this.message = message;
		}

		public MessageSourceResolvable getMessage() {
			return new DefaultMessageSourceResolvable(codes, message);
		}

	}
}
