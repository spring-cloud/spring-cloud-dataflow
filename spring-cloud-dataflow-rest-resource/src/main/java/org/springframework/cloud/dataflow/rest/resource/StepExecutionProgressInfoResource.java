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

package org.springframework.cloud.dataflow.rest.resource;

import org.springframework.batch.core.StepExecution;
import org.springframework.cloud.dataflow.rest.job.StepExecutionHistory;
import org.springframework.hateoas.PagedResources;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.util.Assert;

/**
 * Represents the step execution progress info resource.
 *
 * @author Ilayaperumal Gopinathan
 * @author Glenn Renfro
 * @since 1.0
 */

public class StepExecutionProgressInfoResource extends ResourceSupport {

	private StepExecution stepExecution;

	private StepExecutionHistory stepExecutionHistory;

	private double percentageComplete;

	private boolean finished;

	private double duration;

	public StepExecutionProgressInfoResource() {
	}

	/**
	 * Create a new StepExecutionProgressInfoResource
	 *
	 * @param stepExecution the step execution, must not be null
	 * @param stepExecutionHistory the step execution history, must not be null
	 * @param percentageComplete the percentage complete of the step
	 * @param isFinished whether the step execution is finished
	 * @param duration the duration of the step in milliseconds
	 */
	public StepExecutionProgressInfoResource(StepExecution stepExecution, StepExecutionHistory stepExecutionHistory,
			double percentageComplete, boolean isFinished, double duration) {

		Assert.notNull(stepExecution, "stepExecution must not be null.");
		Assert.notNull(stepExecutionHistory, "stepExecution must not be null.");

		this.stepExecution = stepExecution;
		this.stepExecutionHistory = stepExecutionHistory;
		this.percentageComplete = percentageComplete;
		this.finished = isFinished;
		this.duration = duration;
	}

	public double getPercentageComplete() {
		return percentageComplete;
	}

	public boolean getFinished() {
		return finished;
	}

	public double getDuration() {
		return duration;
	}

	public StepExecution getStepExecution() {
		return stepExecution;
	}

	public StepExecutionHistory getStepExecutionHistory() {
		return stepExecutionHistory;
	}

	public static class Page extends PagedResources<StepExecutionProgressInfoResource> {
	}
}
